@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM  push_to_git.bat
REM  One-click commit + push for the whole Real_Estate_App repo (backend,
REM  android, supabase all live in one git repo). Double-click this file, or
REM  run it from a terminal inside backend/.
REM ============================================================================

set "REMOTE_URL=https://github.com/kannathasan-sify/RealEstateApplication.git"
set "BRANCH=main"

REM Move to the project root (one level up from backend\)
cd /d "%~dp0.."

echo.
echo ==========================================================
echo   Real Estate App - Push to Git
echo   Folder: %cd%
echo   Remote: %REMOTE_URL%
echo ==========================================================
echo.

REM ── First-time repo setup ──────────────────────────────────────────────────
if not exist ".git" (
    echo [1/6] No git repo found here - initializing one...
    git init
    git symbolic-ref HEAD refs/heads/%BRANCH%
) else (
    echo [1/6] Existing git repo found.
)

REM ── Make sure the "origin" remote points at the right place ───────────────
git remote get-url origin >nul 2>&1
if errorlevel 1 (
    echo [2/6] Adding remote "origin" -^> %REMOTE_URL%
    git remote add origin %REMOTE_URL%
) else (
    echo [2/6] Remote "origin" already configured.
)

REM ── Show what would be committed and ask for confirmation ─────────────────
echo.
echo [3/6] Changes detected (respecting .gitignore):
echo ----------------------------------------------------------
git add -A --dry-run
echo ----------------------------------------------------------
echo.
echo IMPORTANT: double-check nothing above looks like a secret
echo (.env files, local.properties, settings.local.json should NOT appear).
echo.
choice /c YN /m "Continue and push these changes"
if errorlevel 2 (
    echo Cancelled. Nothing was committed or pushed.
    goto :end
)

REM ── Stage everything ────────────────────────────────────────────────────────
echo.
echo [4/6] Staging changes...
git add -A

REM ── Commit ───────────────────────────────────────────────────────────────
set "COMMIT_MSG="
set /p COMMIT_MSG="Commit message (leave blank for an auto timestamp): "
if "%COMMIT_MSG%"=="" set "COMMIT_MSG=Update %date% %time%"

echo.
echo [5/6] Committing...
git commit -m "%COMMIT_MSG%"
if errorlevel 1 (
    echo   (Nothing new to commit - continuing in case there are unpushed commits.)
)

REM ── Push (pull+rebase first so a non-empty remote doesn't reject us) ──────
echo.
echo [6/6] Syncing with origin/%BRANCH% and pushing...
git pull origin %BRANCH% --rebase --allow-unrelated-histories >nul 2>&1
git push -u origin %BRANCH%

if errorlevel 1 (
    echo.
    echo ============================================================
    echo   Push failed. Common causes:
    echo   - The GitHub repo doesn't exist yet at that URL - create it
    echo     first at https://github.com/new
    echo   - You're not signed in yet - a browser/credential prompt may
    echo     appear the first time; sign in and re-run this script
    echo   - Remote has changes this script couldn't auto-merge -
    echo     resolve manually with "git status" / "git rebase --continue"
    echo ============================================================
) else (
    echo.
    echo Done! Pushed to %REMOTE_URL% ( %BRANCH% branch^)
)

:end
echo.
pause
