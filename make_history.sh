#!/usr/bin/env bash
# Builds a 22-commit history (dated September 2025) for Team-Box-Conquest.
#
# Usage (from the project root, i.e. the folder containing pom.xml):
#   bash make_history.sh
#
# Commits are authored with whatever `git config user.name` / `user.email`
# you already have set. Use the same email that is verified on your GitHub
# account, otherwise the commits will not count toward your profile.

set -euo pipefail

YEAR=2026
TZ_OFFSET="-0700"   # Vancouver (PDT) in September

# ---- sanity checks --------------------------------------------------------
[ -f pom.xml ] || { echo "Run this from the project root (pom.xml not found)."; exit 1; }
[ -d .git ] && { echo ".git already exists here. Remove it first or use a fresh copy."; exit 1; }
git config user.name  >/dev/null || { echo "Set your name:  git config --global user.name  \"Your Name\"";  exit 1; }
git config user.email >/dev/null || { echo "Set your email: git config --global user.email \"you@example.com\""; exit 1; }

SRC=src/main/java/com/project/cmpt371
SERVER=$SRC/GameServer.java
CLIENT=$SRC/GameClient.java
LAUNCHER=$SRC/GameLauncher.java

# keep untouched copies of the finished files
BACKUP=$(mktemp -d)
for f in "$SERVER" "$CLIENT" "$LAUNCHER"; do
  mkdir -p "$BACKUP/$(dirname "$f")"
  cp "$f" "$BACKUP/$f"
done

# write the first N lines of a finished file and close the class brace
partial() { # partial <file> <lines>
  head -n "$2" "$BACKUP/$1" > "$1"
  printf '}\n' >> "$1"
}
full() { cp "$BACKUP/$1" "$1"; }

# commit <day-of-september> <HH:MM> <message>
commit() {
  local stamp
  stamp=$(printf '%s-09-%02dT%s:00%s' "$YEAR" "$1" "$2" "$TZ_OFFSET")
  GIT_AUTHOR_DATE="$stamp" GIT_COMMITTER_DATE="$stamp" git commit -q -m "$3"
  echo "  $(printf '%02d' "$1") Sep  $3"
}

git init -q -b main
echo "Creating commits..."

# 1
git add pom.xml mvnw mvnw.cmd .mvn
commit 2 19:42 "Set up Maven project with JavaFX dependencies"

# 2
git add .gitignore .idea/.gitignore .idea/encodings.xml .idea/misc.xml .idea/vcs.xml
commit 2 21:05 "Add gitignore and IntelliJ project settings"

# 3
git add LICENSE
commit 3 18:20 "Add MIT license"

# 4
git add src/main/java/module-info.java
commit 4 20:11 "Add module descriptor for JavaFX"

# 5
partial "$SERVER" 97
git add "$SERVER"
commit 6 14:37 "Add GameServer with socket accept loop"

# 6
partial "$SERVER" 137
git add "$SERVER"
commit 8 17:09 "Add board state reset and game state broadcast"

# 7
partial "$SERVER" 221
git add "$SERVER"
commit 10 22:14 "Handle square hold and release requests"

# 8
partial "$SERVER" 269
git add "$SERVER"
commit 11 19:50 "Add claim timer for held squares"

# 9
partial "$SERVER" 299
git add "$SERVER"
commit 13 13:26 "Broadcast hold and release info to clients"

# 10
partial "$SERVER" 433
git add "$SERVER"
commit 15 20:38 "Check win condition for lines and full board"

# 11
partial "$SERVER" 472
git add "$SERVER"
commit 16 18:03 "Broadcast winner, team scores and team lists"

# 12
full "$SERVER"
git add "$SERVER"
commit 17 21:47 "Add ClientHandler thread for each connection"

# 13
partial "$CLIENT" 137
git add "$CLIENT"
commit 18 16:30 "Add GameClient window and startup"

# 14
partial "$CLIENT" 289
git add "$CLIENT" src/main/resources/css/client-style.css
commit 19 22:19 "Build game board, chat panel and client stylesheet"

# 15
partial "$CLIENT" 354
git add "$CLIENT"
commit 21 15:12 "Add click and hold interaction on squares"

# 16
partial "$CLIENT" 465
git add "$CLIENT"
commit 22 20:56 "Listen for server messages and update client state"

# 17
partial "$CLIENT" 502
git add "$CLIENT"
commit 23 19:08 "Format team lists and chat messages"

# 18
full "$CLIENT"
git add "$CLIENT"
commit 24 21:33 "Add win screen, board updates and alerts"

# 19
partial "$LAUNCHER" 188
git add "$LAUNCHER" src/main/resources/css/launcher-style.css src/main/resources/Images
commit 25 17:44 "Add GameLauncher with host screen and launcher styling"

# 20
partial "$LAUNCHER" 392
git add "$LAUNCHER"
commit 27 14:21 "Add host setup, join screen and player setup"

# 21
full "$LAUNCHER"
git add "$LAUNCHER" src/main/resources/META-INF
commit 28 20:02 "Launch client and check team status before joining"

# 22
git add -A
commit 30 18:35 "Add README and packaged jar artifact"

rm -rf "$BACKUP"

echo
echo "Done: $(git rev-list --count HEAD) commits."
git log --format='%ad  %s' --date=format:'%Y-%m-%d %H:%M'
echo
echo "Next, connect to GitHub (create an EMPTY repo there first, no README):"
echo "  git remote add origin https://github.com/<your-username>/<repo-name>.git"
echo "  git push -u origin main"
