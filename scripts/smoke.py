"""Run against a running compose stack. Python 3, standard library only."""

import json
import os
import time
import uuid
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from urllib.parse import urlencode

base = os.environ.get('SHOWCASE_URL', 'http://127.0.0.1:8080').rstrip('/')
url = f'{base}/topics/{uuid.uuid4().hex}/events'


def request(**params):
    req = Request(
        url + ('?' + urlencode(params) if params else ''),
        method='POST' if params else 'GET',
    )
    try:
        with urlopen(req, timeout=8) as res:
            return res.status, res.read()
    except HTTPError as err:
        return err.code, err.read()


def history(expected, timeout=15):
    stop = time.monotonic() + timeout
    last = None
    while time.monotonic() < stop:
        try:
            status, last = request()
            if status == 200 and json.loads(last) == expected:
                return
        except (URLError, ConnectionError, TimeoutError) as exc:
            last = exc
        time.sleep(0.2)
    raise RuntimeError(f'expected {expected}, last response: {last!r}')


def post(status, **params):
    actual, body = request(**params)
    if actual != status:
        raise RuntimeError(f'{params}: expected {status}, got {actual}: {body!r}')


history([], timeout=120)
post(400, eventId='')
post(200, eventId='a')
post(409, eventId='a')
post(202, eventId='b', mode='async')
history(['a', 'b'])
for event in ['c', 'd']:
    post(202, eventId=event, mode='ordered')
history(list('abcd'))
expected = list('abcd')
for mode, event in [('delay', 'e'), ('delayedOrdered', 'f')]:
    before = time.monotonic()
    post(202, eventId=event, mode=mode, delayMs=1500)
    expected.append(event)
    history(expected)
    if time.monotonic() - before < 1.4:
        raise RuntimeError(f'{mode} delivered too early')
print('OK: sync, validation, duplicate, async, partition order, delay and partitioned delay')
