from __future__ import annotations

from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

from .paths import WEB_SITE_DIR


class NoCacheStaticHandler(SimpleHTTPRequestHandler):
    def end_headers(self) -> None:
        self.send_header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
        self.send_header("Pragma", "no-cache")
        self.send_header("Expires", "0")
        super().end_headers()


def serve_site(*, port: int) -> None:
    handler = partial(NoCacheStaticHandler, directory=str(WEB_SITE_DIR))
    server = ThreadingHTTPServer(("127.0.0.1", port), handler)

    print(f"Serving {WEB_SITE_DIR} at http://127.0.0.1:{port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
