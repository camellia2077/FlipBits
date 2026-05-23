from __future__ import annotations

import argparse
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Serve the audio_web static site locally.")
    parser.add_argument(
        "--port",
        type=int,
        default=4173,
        help="Port to listen on. Default: 4173",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    site_dir = Path(__file__).resolve().parents[1] / "site"
    handler = partial(SimpleHTTPRequestHandler, directory=str(site_dir))
    server = ThreadingHTTPServer(("127.0.0.1", args.port), handler)

    print(f"Serving {site_dir} at http://127.0.0.1:{args.port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
