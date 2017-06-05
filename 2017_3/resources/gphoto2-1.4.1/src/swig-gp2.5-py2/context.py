# This file was automatically generated by SWIG (http://www.swig.org).
# Version 3.0.7
#
# Do not make changes to this file unless you know what you are doing--modify
# the SWIG interface file instead.





from sys import version_info
if version_info >= (3, 0, 0):
    new_instancemethod = lambda func, inst, cls: _context.SWIG_PyInstanceMethod_New(func)
else:
    from new import instancemethod as new_instancemethod
if version_info >= (2, 6, 0):
    def swig_import_helper():
        from os.path import dirname
        import imp
        fp = None
        try:
            fp, pathname, description = imp.find_module('_context', [dirname(__file__)])
        except ImportError:
            import _context
            return _context
        if fp is not None:
            try:
                _mod = imp.load_module('_context', fp, pathname, description)
            finally:
                fp.close()
            return _mod
    _context = swig_import_helper()
    del swig_import_helper
else:
    import _context
del version_info
try:
    _swig_property = property
except NameError:
    pass  # Python < 2.2 doesn't have 'property'.


def _swig_setattr_nondynamic(self, class_type, name, value, static=1):
    if (name == "thisown"):
        return self.this.own(value)
    if (name == "this"):
        if type(value).__name__ == 'SwigPyObject':
            self.__dict__[name] = value
            return
    method = class_type.__swig_setmethods__.get(name, None)
    if method:
        return method(self, value)
    if (not static):
        object.__setattr__(self, name, value)
    else:
        raise AttributeError("You cannot add attributes to %s" % self)


def _swig_setattr(self, class_type, name, value):
    return _swig_setattr_nondynamic(self, class_type, name, value, 0)


def _swig_getattr_nondynamic(self, class_type, name, static=1):
    if (name == "thisown"):
        return self.this.own()
    method = class_type.__swig_getmethods__.get(name, None)
    if method:
        return method(self)
    if (not static):
        return object.__getattr__(self, name)
    else:
        raise AttributeError(name)

def _swig_getattr(self, class_type, name):
    return _swig_getattr_nondynamic(self, class_type, name, 0)


def _swig_repr(self):
    try:
        strthis = "proxy of " + self.this.__repr__()
    except:
        strthis = ""
    return "<%s.%s; %s >" % (self.__class__.__module__, self.__class__.__name__, strthis,)

try:
    _object = object
    _newclass = 1
except AttributeError:
    class _object:
        pass
    _newclass = 0



def _swig_setattr_nondynamic_method(set):
    def set_attr(self, name, value):
        if (name == "thisown"):
            return self.this.own(value)
        if hasattr(self, name) or (name == "this"):
            set(self, name, value)
        else:
            raise AttributeError("You cannot add attributes to %s" % self)
    return set_attr


import gphoto2.list
class Context(object):
    """Proxy of C _GPContext struct"""
    thisown = _swig_property(lambda x: x.this.own(), lambda x, v: x.this.own(v), doc='The membership flag')
    __repr__ = _swig_repr

    def __init__(self):
        """__init__(self) -> Context"""
        _context.Context_swiginit(self, _context.new_Context())
    __swig_destroy__ = _context.delete_Context

    def camera_autodetect(self):
        """
        camera_autodetect(self)

        Parameters:
            self: struct _GPContext *


        See also: gphoto2.gp_camera_autodetect
        """
        return _context.Context_camera_autodetect(self)

Context.camera_autodetect = new_instancemethod(_context.Context_camera_autodetect, None, Context)
Context_swigregister = _context.Context_swigregister
Context_swigregister(Context)


def gp_context_new():
    """gp_context_new() -> Context"""
    return _context.gp_context_new()

_context.GP_CONTEXT_FEEDBACK_OK_swigconstant(_context)
GP_CONTEXT_FEEDBACK_OK = _context.GP_CONTEXT_FEEDBACK_OK

_context.GP_CONTEXT_FEEDBACK_CANCEL_swigconstant(_context)
GP_CONTEXT_FEEDBACK_CANCEL = _context.GP_CONTEXT_FEEDBACK_CANCEL

def gp_context_set_idle_func(context, func, data):
    """
    gp_context_set_idle_func(context, func, data)

    Parameters:
        context: Context
        func: GPContextIdleFunc
        data: object

    """
    return _context.gp_context_set_idle_func(context, func, data)

def gp_context_set_progress_funcs(context, start_func, update_func, stop_func, data):
    """
    gp_context_set_progress_funcs(context, start_func, update_func, stop_func, data)

    Parameters:
        context: Context
        start_func: GPContextProgressStartFunc
        update_func: GPContextProgressUpdateFunc
        stop_func: GPContextProgressStopFunc
        data: object

    """
    return _context.gp_context_set_progress_funcs(context, start_func, update_func, stop_func, data)

def gp_context_set_error_func(context, func, data):
    """
    gp_context_set_error_func(context, func, data)

    Parameters:
        context: Context
        func: GPContextErrorFunc
        data: object

    """
    return _context.gp_context_set_error_func(context, func, data)

def gp_context_set_status_func(context, func, data):
    """
    gp_context_set_status_func(context, func, data)

    Parameters:
        context: Context
        func: GPContextStatusFunc
        data: object

    """
    return _context.gp_context_set_status_func(context, func, data)

def gp_context_set_question_func(context, func, data):
    """
    gp_context_set_question_func(context, func, data)

    Parameters:
        context: Context
        func: GPContextQuestionFunc
        data: object

    """
    return _context.gp_context_set_question_func(context, func, data)

def gp_context_set_cancel_func(context, func, data):
    """
    gp_context_set_cancel_func(context, func, data)

    Parameters:
        context: Context
        func: GPContextCancelFunc
        data: object

    """
    return _context.gp_context_set_cancel_func(context, func, data)

def gp_context_set_message_func(context, func, data):
    """
    gp_context_set_message_func(context, func, data)

    Parameters:
        context: Context
        func: GPContextMessageFunc
        data: object

    """
    return _context.gp_context_set_message_func(context, func, data)

def gp_context_idle(context):
    """
    gp_context_idle(context)

    Parameters:
        context: Context

    """
    return _context.gp_context_idle(context)

def gp_context_error(context, format):
    """
    gp_context_error(context, format)

    Parameters:
        context: Context
        format: str

    """
    return _context.gp_context_error(context, format)

def gp_context_status(context, format):
    """
    gp_context_status(context, format)

    Parameters:
        context: Context
        format: str

    """
    return _context.gp_context_status(context, format)

def gp_context_message(context, format):
    """
    gp_context_message(context, format)

    Parameters:
        context: Context
        format: str

    """
    return _context.gp_context_message(context, format)

def gp_context_question(context, format):
    """
    gp_context_question(context, format) -> GPContextFeedback

    Parameters:
        context: Context
        format: str

    """
    return _context.gp_context_question(context, format)

def gp_context_cancel(context):
    """
    gp_context_cancel(context) -> GPContextFeedback

    Parameters:
        context: Context

    """
    return _context.gp_context_cancel(context)

def gp_context_progress_start(context, target, format):
    """
    gp_context_progress_start(context, target, format) -> unsigned int

    Parameters:
        context: Context
        target: float
        format: str

    """
    return _context.gp_context_progress_start(context, target, format)

def gp_context_progress_update(context, id, current):
    """
    gp_context_progress_update(context, id, current)

    Parameters:
        context: Context
        id: unsigned int
        current: float

    """
    return _context.gp_context_progress_update(context, id, current)

def gp_context_progress_stop(context, id):
    """
    gp_context_progress_stop(context, id)

    Parameters:
        context: Context
        id: unsigned int

    """
    return _context.gp_context_progress_stop(context, id)


