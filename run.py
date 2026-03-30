"""
run.py — Entry point that fixes Windows event loop before uvicorn starts.
Use:  python run.py
"""
import asyncio
import sys

# Must be set before uvicorn imports anything — fixes supabase-py DNS resolution
# on Windows where ProactorEventLoop breaks synchronous httpx socket calls.
if sys.platform == "win32":
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

import uvicorn

if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=False)
