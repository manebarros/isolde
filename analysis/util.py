import matplotlib.pyplot as plt
import matplotlib.ticker as ticker

HEIGHT = 1.2   # The height of each plot
WIDTH = 6.3    # The width of a row of plots
HSPACE = 0.08  # The horizontal space between plots
VSPACE = 0.05  # The vertical space between plots

LEVELS = ["RA", "UA", "CC", "PC", "PSI", "SI", "SER"]
PRIORITY_DICT = dict(zip(LEVELS, list(range(0, 8)))) 

BLUE = "#1f77b4"
ORANGE = "#ff7f0e"
GREEN = "#2ca02c"

STYLE = {
    "isolde" : ('s', '--'),
    "default_improved" : ('^', ':'),
    "direct" : ('s', '--'),
    "directDefault" : ('s', '--'),
    "abstractImprovedSmallerFormula" : ('^', ':'),
    "alloystar" : ('o', '-'),
    "bounds_based_simple" : ('^', ':'),
    "optimized" : ('s', '--'),
    "not_optimized" : ('^', ':'),
}

def id(x):
    return x

def calculate_ord_fun(sequence):
    priority_dict = dict(zip(sequence, list(range(0, len(sequence)))))
    return lambda x: priority_dict[x] if x in priority_dict else -1

def priority(level):
    return calculate_ord_fun(LEVELS)(level)

def rename_solver(solver_name):
    if solver_name == "glucose":
        return "Glucose"
    elif solver_name == "minisat":
        return "MiniSat"
    else:
        return "Sat4j"

def determine_unit(max_val):
    if max_val < 1500:
        return 1, 'ms' #r'\unit{\milli\second}'
    else:
        return 1000, 's' #r'\unit{\second}'

def get_label_colors(labels):
    return dict(zip(labels, plt.cm.tab10.colors))

def get_formatter(scale_factor):
    return (lambda v, _: f'{v / scale_factor:.1f}')

def plotMatrixTimePerTxn(data, row_field, col_field, target_field,
                         row_display_func, column_display_func, label_names, label_colors, 
                         logScaling=False, row_ord_func=None, label_ord_fun=None, legendSize=(3, 0.95)):
    draw_row_fun = rowTimeLog if logScaling else rowTimeLinear
    return plotMatrix(data, row_field, col_field, target_field, 'txn', 'avg_time', ('min_time', 'max_time'),
                      row_display_func, column_display_func, 'Number of transactions', label_names, label_colors, draw_row_fun, 
                      row_ord_func, label_ord_fun, legendSize)

def plotMatrixCandidatesPerTxn(
    data, row_field, col_field, target_field,
    row_display_func, column_display_func, label_names, label_colors, 
    logScaling=False, row_ord_func=None, label_ord_fun=None, legendSize=(3, 0.95)):
    
    draw_row_fun = rowCandLog if logScaling else rowCandLinear
    return plotMatrix(
        data, row_field, col_field, target_field, 'txn', 'avg_cand', ('min_cand', 'max_cand'),
        row_display_func, column_display_func, 'Number of transactions', label_names, 
        label_colors, draw_row_fun, row_ord_func, label_ord_fun, legendSize)


def plotMatrix(data, row_field, col_field, target_field, x_field, y_field, error_fields,
               row_display_func, column_display_func, xaxis_name, label_names, label_colors, draw_row_fun,
               row_ord_func=None, label_ord_fun=None, legendSize=None):
    n_cols = data[col_field].nunique()
    n_rows = data[row_field].nunique() if row_field else 1

    if row_field:
        grouped = data.groupby(row_field)
        if row_ord_func != None:
            grouped = sorted(grouped, key=lambda x: row_ord_func(x[0]))
    else:
        grouped = data


    figure_size = (HEIGHT * n_rows)/legendSize[1] if legendSize else HEIGHT*n_rows
    fig, axes = plt.subplots(nrows=n_rows, ncols=n_cols, figsize=(WIDTH, figure_size), sharex=True)

    to_iterate = grouped if row_field else [(None, grouped)]

    for i, (row, row_data) in enumerate(to_iterate):
        row_axes = axes if n_rows == 1 else axes[i]
        if i > 0:
            column_display_func = None
        row_name = row_display_func(row) if n_rows > 1 else None
        draw_row_fun(row_data, col_field, row_axes, target_field, x_field, y_field, error_fields,
                label_names, label_colors, column_display_func, label_ord_fun=label_ord_fun, yaxis_name=row_name)

        if i == n_rows-1:
            middle = n_cols // 2
            row_axes[middle].set_xlabel(xaxis_name)
            if n_cols % 2 == 0:
                row_axes[0].set_xlabel(xaxis_name)
                #row_axes[middle+1].set_xlabel(xaxis_name)


    if legendSize:
        # Collect handles and labels from both subplots
        handles, labels = [], []
        for ax in axes.flatten():
            h, l = ax.get_legend_handles_labels()
            handles.extend(h)
            labels.extend(l)
        
        # Use a dictionary to remove duplicates
        if label_ord_fun:
            unique_handles_labels = dict(sorted(zip(labels, handles), key=lambda x: label_ord_fun(x[0])))
        else:
            unique_handles_labels = dict(zip(labels, handles))

        print(unique_handles_labels)

        # Create a common legend with unique labels
        fig.legend(unique_handles_labels.values(), unique_handles_labels.keys(), loc='upper center', ncol=legendSize[0])
    else:
        ax = axes.flatten()[0]
        print(ax)
        ax.legend()

    fig.tight_layout()
    fig.subplots_adjust(wspace=HSPACE)
    fig.subplots_adjust(hspace=VSPACE)
    if legendSize:
        fig.subplots_adjust(top=legendSize[1])
    return fig


def drawAx(ax, data, x_field, y_field, target_field, error_fields, label_names, label_colors,
           label_ord_fun=None):
    target_groups = data.groupby(target_field)
    if (label_ord_fun != None):
        target_groups = sorted(target_groups, key=lambda x: label_ord_fun(x[0]))
    for val, val_data in target_groups:
        if error_fields:
            lower = val_data[y_field] - val_data[error_fields[0]]
            upper = val_data[error_fields[1]] - val_data[y_field]
            errorbar = [lower, upper]
        else:
            errorbar = None
        if val in STYLE.keys():
            ax.errorbar(val_data[x_field], val_data[y_field], yerr=errorbar,
                        linestyle=STYLE[val][1], marker=STYLE[val][0], label=label_names[val], markersize=5, color=label_colors[val])
        else:
            ax.errorbar(val_data[x_field], val_data[y_field], yerr=errorbar,
                        fmt='-o', label=label_names[val], markersize=5, color=label_colors[val])


def rowTimeLinear(row_data, col_field, axes, target_field, x_field, y_field, error_fields,
                  label_names, label_colors, col_name_fun=None, label_ord_fun=None, yaxis_name=None):
    return rowTime(row_data, col_field, axes, target_field, x_field, y_field, error_fields,
                label_names, label_colors, col_name_fun, False, label_ord_fun, yaxis_name)

def rowTimeLog(row_data, col_field, axes, target_field, x_field, y_field, error_fields,
                  label_names, label_colors, col_name_fun=None, label_ord_fun=None, yaxis_name=None):
    return rowTime(row_data, col_field, axes, target_field, x_field, y_field, error_fields,
                label_names, label_colors, col_name_fun, True, label_ord_fun, yaxis_name)

def rowTime(row_data, col_field, axes, target_field, x_field, y_field, error_fields,
                label_names, label_colors, col_name_fun=None, logScaling=False, label_ord_fun=None, yaxis_name=None):
    min_val = row_data[error_fields[0] if error_fields else y_field].min()
    max_val = row_data[error_fields[1] if error_fields else y_field].max()

    scale_factor, unit = determine_unit(max_val)

    col_groups = row_data.groupby(col_field)
    for j, (col, col_data) in enumerate(col_groups):
        ax = axes[j]
        drawAx(ax, col_data, x_field, y_field, target_field, error_fields, label_names, label_colors, label_ord_fun)

        if col_name_fun:
            ax.set_title(col_name_fun(col))

        if logScaling:
            ax.set_yscale("log")
            ax.set_ylim(bottom=(min_val - 0.1 * min_val), top=(max_val + 0.2 * max_val))
        else:
            ax.set_ylim(0, (max_val + 0.05 * max_val))

        ax.yaxis.set_major_formatter(get_formatter(scale_factor))
        ax.xaxis.set_major_locator(ticker.MultipleLocator(2))

        if j > 0:
            ax.set_yticklabels([])
        else:
            ax.set_ylabel((f'{yaxis_name}\n\n' if yaxis_name else '') + f'Time ({unit})')

        ax.grid(True)


def rowCandLinear(row_data, col_field, axes, target_field, x_field, y_field, error_fields,
                label_names, label_colors, col_name_fun=None, label_ord_fun=None, yaxis_name=None):
    return rowCandidates(row_data, col_field, axes, target_field, x_field, y_field, error_fields,
                label_names, label_colors, col_name_fun=col_name_fun, logScaling=False, label_ord_fun=label_ord_fun, yaxis_name=yaxis_name)

def rowCandLog(row_data, col_field, axes, target_field, x_field, y_field, error_fields,
                label_names, label_colors, col_name_fun=None, label_ord_fun=None, yaxis_name=None):
    return rowCandidates(row_data, col_field, axes, target_field, x_field, y_field, error_fields,
                label_names, label_colors, col_name_fun=col_name_fun, logScaling=True, label_ord_fun=label_ord_fun, yaxis_name=yaxis_name)

def rowCandidates(row_data, col_field, axes, target_field, x_field, y_field, error_fields,
                label_names, label_colors, col_name_fun=None, logScaling=False, label_ord_fun=None, yaxis_name=None):
    min_val = row_data[error_fields[0] if error_fields else y_field].min()
    max_val = row_data[error_fields[1] if error_fields else y_field].max()

    col_groups = row_data.groupby(col_field)
    for j, (col, col_data) in enumerate(col_groups):
        ax = axes[j]
        drawAx(ax, col_data, x_field, y_field, target_field, error_fields, label_names, label_colors, label_ord_fun)

        if col_name_fun:
            ax.set_title(col_name_fun(col))

        if max_val < 20: 
            ax.yaxis.set_major_locator(ticker.MultipleLocator(5))

        if logScaling:
            ax.set_yscale("log")
            ax.set_ylim(bottom=(min_val - 0.1 * min_val), top=(max_val + 0.1 * max_val))
        else:
            ax.set_ylim(0, (max_val + 0.05 * max_val))

        if j > 0:
            ax.set_yticklabels([])
        else:
            ax.set_ylabel((f'{yaxis_name}\n' if yaxis_name else '') + 'Number of candidates')

        ax.grid(True)
