import logging
from StringIO import StringIO

stream = StringIO()
handler = logging.StreamHandler(stream)
formatter = logging.Formatter('%(asctime)s | %(message)s',
                              '%d-%b-%Y | %H:%M:%S')
handler.setFormatter(formatter)
log = logging.getLogger('mylogger')
log.addHandler(handler)

#log.setLevel(logging.INFO)
for handler in log.handlers: 
    log.removeHandler(handler)
log.addHandler(handler)

log.error("ERROR: TEST")
handler.flush()
print '[', stream.getvalue(), ']'
