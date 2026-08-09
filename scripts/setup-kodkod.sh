#!/usr/bin/env bash
#
# Installs Kodkod into the local Maven repository, plus the native SAT solvers it can drive.
#
# Kodkod is not published to Maven Central or to any other public repository, so `mvn package`
# cannot resolve it on its own: it has to be placed in the local repository by hand. This script
# does that from the upstream GitHub release, checking every download against a pinned SHA-256 so
# that every machine ends up with the identical artifact.
#
# Usage:  scripts/setup-kodkod.sh [extra maven args...]
#
# Extra arguments are passed to Maven, which is how a non-default local repository is selected:
#
#     scripts/setup-kodkod.sh -Dmaven.repo.local=/tmp/m2
#
set -euo pipefail

KODKOD_VERSION=2.1
GROUP_ID=com.github.emina
ARTIFACT_ID=kodkod
BASE_URL="https://github.com/emina/kodkod/releases/download/v${KODKOD_VERSION}"

# Pinned checksums of the release assets. A mismatch aborts the install rather than working with
# an artifact we cannot identify.
JAR_SHA256=2d689ebba004456a53de23283bc270d81283d761d0ecdbabd0f8acad1f78d372
LINUX_X86_64_SHA256=77f59447453df4e6a50d37eef3197185da32ec4485c785af47cb2c9cda5cbc6f
DARWIN_X86_64_SHA256=43c95b68902241d0bcb02ad9fbf436058aa136450c7f01532e864ab564ccccea

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE_DIR="$REPO_ROOT/lib/.cache"
NATIVE_DIR="$REPO_ROOT/lib/native"

die() {
  echo "error: $*" >&2
  exit 1
}

sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d' ' -f1
  else
    shasum -a 256 "$1" | cut -d' ' -f1
  fi
}

# Downloads $1 to $2 unless a copy with the expected checksum $3 is already there. A download whose
# checksum does not match is discarded, never left behind to be picked up by a later run.
fetch() {
  local url="$1" dest="$2" expected="$3" actual

  if [[ -f "$dest" && "$(sha256_of "$dest")" == "$expected" ]]; then
    echo "  $(basename "$dest"): already downloaded"
    return
  fi

  echo "  $(basename "$dest"): downloading"
  curl -fsSL -o "$dest.part" "$url" || die "could not download $url"

  actual="$(sha256_of "$dest.part")"
  if [[ "$actual" != "$expected" ]]; then
    rm -f "$dest.part"
    die "checksum mismatch for $url
  expected $expected
  got      $actual
The release asset is not what this script was pinned against. Nothing was installed."
  fi
  mv "$dest.part" "$dest"
}

mkdir -p "$CACHE_DIR"

echo "Kodkod $KODKOD_VERSION -> $GROUP_ID:$ARTIFACT_ID:$KODKOD_VERSION"
fetch "$BASE_URL/kodkod.jar" "$CACHE_DIR/kodkod-$KODKOD_VERSION.jar" "$JAR_SHA256"

# Prefer the wrapper, so the artifact is installed by the same Maven the build will use.
MVN="$REPO_ROOT/mvnw"
[[ -x "$MVN" ]] || MVN=mvn

"$MVN" "$@" -q install:install-file \
  -Dfile="$CACHE_DIR/kodkod-$KODKOD_VERSION.jar" \
  -DgroupId="$GROUP_ID" \
  -DartifactId="$ARTIFACT_ID" \
  -Dversion="$KODKOD_VERSION" \
  -Dpackaging=jar
echo "  installed"

# The native solvers are separate release assets, not part of the jar. Without them Kodkod can only
# use Sat4j, which is pure Java. Upstream ships prebuilt libraries for x86-64 Linux and macOS only.
echo
echo "Native SAT solvers (minisat, glucose)"
case "$(uname -s):$(uname -m)" in
  Linux:x86_64) platform=linux_x86_64 expected="$LINUX_X86_64_SHA256" ;;
  Darwin:x86_64) platform=darwin_x86_64 expected="$DARWIN_X86_64_SHA256" ;;
  *) platform="" ;;
esac

if [[ -z "$platform" ]]; then
  echo "  no prebuilt libraries for $(uname -s) $(uname -m); use --solver sat4j"
  echo
  echo "Done. Kodkod is installed, so the build will work."
  exit 0
fi

fetch "$BASE_URL/$platform.zip" "$CACHE_DIR/$platform.zip" "$expected"
mkdir -p "$NATIVE_DIR"
unzip -oq "$CACHE_DIR/$platform.zip" -d "$NATIVE_DIR"
echo "  installed into lib/native/$platform"

echo
echo "Done. To use --solver minisat or --solver glucose, put the libraries on the library path:"
echo
echo "    java -Djava.library.path=$NATIVE_DIR/$platform -jar cli/target/isolde.jar ..."
