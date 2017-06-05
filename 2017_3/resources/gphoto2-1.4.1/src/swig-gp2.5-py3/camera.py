# This file was automatically generated by SWIG (http://www.swig.org).
# Version 3.0.7
#
# Do not make changes to this file unless you know what you are doing--modify
# the SWIG interface file instead.





from sys import version_info
if version_info >= (3, 0, 0):
    new_instancemethod = lambda func, inst, cls: _camera.SWIG_PyInstanceMethod_New(func)
else:
    from new import instancemethod as new_instancemethod
if version_info >= (2, 6, 0):
    def swig_import_helper():
        from os.path import dirname
        import imp
        fp = None
        try:
            fp, pathname, description = imp.find_module('_camera', [dirname(__file__)])
        except ImportError:
            import _camera
            return _camera
        if fp is not None:
            try:
                _mod = imp.load_module('_camera', fp, pathname, description)
            finally:
                fp.close()
            return _mod
    _camera = swig_import_helper()
    del swig_import_helper
else:
    import _camera
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


import gphoto2.abilities_list
import gphoto2.context
import gphoto2.list
import gphoto2.port_info_list
import gphoto2.port
import gphoto2.port_log
import gphoto2.filesys
import gphoto2.file
import gphoto2.result
import gphoto2.widget

def gp_camera_capture_preview(camera: 'Camera', context: 'Context') -> "CameraFile *":
    """
    gp_camera_capture_preview(camera, context) -> int

    Parameters:
        camera: Camera
        context: Context

    """
    return _camera.gp_camera_capture_preview(camera, context)
class CameraText(object):
    """Proxy of C CameraText struct"""
    thisown = _swig_property(lambda x: x.this.own(), lambda x, v: x.this.own(v), doc='The membership flag')

    def __init__(self, *args, **kwargs):
        raise AttributeError("No constructor defined")
    __repr__ = _swig_repr

    def __str__(self) -> "char *":
        """
        __str__(self) -> char *

        Parameters:
            self: CameraText *

        """
        return _camera.CameraText___str__(self)

    text = _swig_property(_camera.CameraText_text_get)
    __swig_destroy__ = _camera.delete_CameraText
CameraText.__str__ = new_instancemethod(_camera.CameraText___str__, None, CameraText)
CameraText_swigregister = _camera.CameraText_swigregister
CameraText_swigregister(CameraText)

class CameraFilePath(object):
    """Proxy of C CameraFilePath struct"""
    thisown = _swig_property(lambda x: x.this.own(), lambda x, v: x.this.own(v), doc='The membership flag')

    def __init__(self, *args, **kwargs):
        raise AttributeError("No constructor defined")
    __repr__ = _swig_repr
    name = _swig_property(_camera.CameraFilePath_name_get)
    folder = _swig_property(_camera.CameraFilePath_folder_get)
    __swig_destroy__ = _camera.delete_CameraFilePath
CameraFilePath_swigregister = _camera.CameraFilePath_swigregister
CameraFilePath_swigregister(CameraFilePath)


_camera.GP_CAPTURE_IMAGE_swigconstant(_camera)
GP_CAPTURE_IMAGE = _camera.GP_CAPTURE_IMAGE

_camera.GP_CAPTURE_MOVIE_swigconstant(_camera)
GP_CAPTURE_MOVIE = _camera.GP_CAPTURE_MOVIE

_camera.GP_CAPTURE_SOUND_swigconstant(_camera)
GP_CAPTURE_SOUND = _camera.GP_CAPTURE_SOUND

_camera.GP_EVENT_UNKNOWN_swigconstant(_camera)
GP_EVENT_UNKNOWN = _camera.GP_EVENT_UNKNOWN

_camera.GP_EVENT_TIMEOUT_swigconstant(_camera)
GP_EVENT_TIMEOUT = _camera.GP_EVENT_TIMEOUT

_camera.GP_EVENT_FILE_ADDED_swigconstant(_camera)
GP_EVENT_FILE_ADDED = _camera.GP_EVENT_FILE_ADDED

_camera.GP_EVENT_FOLDER_ADDED_swigconstant(_camera)
GP_EVENT_FOLDER_ADDED = _camera.GP_EVENT_FOLDER_ADDED

_camera.GP_EVENT_CAPTURE_COMPLETE_swigconstant(_camera)
GP_EVENT_CAPTURE_COMPLETE = _camera.GP_EVENT_CAPTURE_COMPLETE
class Camera(object):
    """Proxy of C _Camera struct"""
    thisown = _swig_property(lambda x: x.this.own(), lambda x, v: x.this.own(v), doc='The membership flag')
    __repr__ = _swig_repr

    def __init__(self):
        """__init__(self) -> Camera"""
        _camera.Camera_swiginit(self, _camera.new_Camera())
    __swig_destroy__ = _camera.delete_Camera

    def set_abilities(self, abilities: 'CameraAbilities') -> "void":
        """
        set_abilities(self, abilities)

        Parameters:
            abilities: CameraAbilities


        See also: gphoto2.gp_camera_set_abilities
        """
        return _camera.Camera_set_abilities(self, abilities)


    def get_abilities(self) -> "void":
        """
        get_abilities(self)

        Parameters:
            self: struct _Camera *


        See also: gphoto2.gp_camera_get_abilities
        """
        return _camera.Camera_get_abilities(self)


    def set_port_info(self, info: '_GPPortInfo') -> "void":
        """
        set_port_info(self, info)

        Parameters:
            info: GPPortInfo


        See also: gphoto2.gp_camera_set_port_info
        """
        return _camera.Camera_set_port_info(self, info)


    def get_port_info(self) -> "void":
        """
        get_port_info(self)

        Parameters:
            self: struct _Camera *


        See also: gphoto2.gp_camera_get_port_info
        """
        return _camera.Camera_get_port_info(self)


    def set_port_speed(self, speed: 'int') -> "void":
        """
        set_port_speed(self, speed)

        Parameters:
            speed: int


        See also: gphoto2.gp_camera_set_port_speed
        """
        return _camera.Camera_set_port_speed(self, speed)


    def get_port_speed(self) -> "int":
        """
        get_port_speed(self) -> int

        Parameters:
            self: struct _Camera *


        See also: gphoto2.gp_camera_get_port_speed
        """
        return _camera.Camera_get_port_speed(self)


    def init(self, context: 'Context') -> "void":
        """
        init(self, context)

        Parameters:
            context: Context


        See also: gphoto2.gp_camera_init
        """
        return _camera.Camera_init(self, context)


    def exit(self, context: 'Context') -> "void":
        """
        exit(self, context)

        Parameters:
            context: Context


        See also: gphoto2.gp_camera_exit
        """
        return _camera.Camera_exit(self, context)


    def get_config(self, context: 'Context') -> "void":
        """
        get_config(self, context)

        Parameters:
            context: Context


        See also: gphoto2.gp_camera_get_config
        """
        return _camera.Camera_get_config(self, context)


    def set_config(self, window: 'CameraWidget', context: 'Context') -> "void":
        """
        set_config(self, window, context)

        Parameters:
            window: CameraWidget
            context: Context


        See also: gphoto2.gp_camera_set_config
        """
        return _camera.Camera_set_config(self, window, context)


    def get_summary(self, context: 'Context') -> "void":
        """
        get_summary(self, context)

        Parameters:
            context: Context


        See also: gphoto2.gp_camera_get_summary
        """
        return _camera.Camera_get_summary(self, context)


    def get_manual(self, context: 'Context') -> "void":
        """
        get_manual(self, context)

        Parameters:
            context: Context


        See also: gphoto2.gp_camera_get_manual
        """
        return _camera.Camera_get_manual(self, context)


    def get_about(self, context: 'Context') -> "void":
        """
        get_about(self, context)

        Parameters:
            context: Context


        See also: gphoto2.gp_camera_get_about
        """
        return _camera.Camera_get_about(self, context)


    def capture(self, type: 'CameraCaptureType', context: 'Context') -> "void":
        """
        capture(self, type, context)

        Parameters:
            type: enum CameraCaptureType
            context: Context


        See also: gphoto2.gp_camera_capture
        """
        return _camera.Camera_capture(self, type, context)


    def trigger_capture(self, context: 'Context') -> "void":
        """
        trigger_capture(self, context)

        Parameters:
            context: Context


        See also: gphoto2.gp_camera_trigger_capture
        """
        return _camera.Camera_trigger_capture(self, context)


    def capture_preview(self, context: 'Context') -> "void":
        """
        capture_preview(self, context)

        Parameters:
            context: Context


        See also: gphoto2.gp_camera_capture_preview
        """
        return _camera.Camera_capture_preview(self, context)


    def wait_for_event(self, timeout: 'int', context: 'Context') -> "void":
        """
        wait_for_event(self, timeout, context)

        Parameters:
            timeout: int
            context: Context


        See also: gphoto2.gp_camera_wait_for_event
        """
        return _camera.Camera_wait_for_event(self, timeout, context)


    def get_storageinfo(self, context: 'Context') -> "void":
        """
        get_storageinfo(self, context)

        Parameters:
            context: Context


        See also: gphoto2.gp_camera_get_storageinfo
        """
        return _camera.Camera_get_storageinfo(self, context)


    def folder_list_files(self, folder: 'char const *', context: 'Context') -> "void":
        """
        folder_list_files(self, folder, context)

        Parameters:
            folder: str
            context: Context


        See also: gphoto2.gp_camera_folder_list_files
        """
        return _camera.Camera_folder_list_files(self, folder, context)


    def folder_list_folders(self, folder: 'char const *', context: 'Context') -> "void":
        """
        folder_list_folders(self, folder, context)

        Parameters:
            folder: str
            context: Context


        See also: gphoto2.gp_camera_folder_list_folders
        """
        return _camera.Camera_folder_list_folders(self, folder, context)


    def folder_delete_all(self, folder: 'char const *', context: 'Context') -> "void":
        """
        folder_delete_all(self, folder, context)

        Parameters:
            folder: str
            context: Context


        See also: gphoto2.gp_camera_folder_delete_all
        """
        return _camera.Camera_folder_delete_all(self, folder, context)


    def folder_put_file(self, folder: 'char const *', filename: 'char const *', type: 'CameraFileType', file: 'CameraFile', context: 'Context') -> "void":
        """
        folder_put_file(self, folder, filename, type, file, context)

        Parameters:
            folder: str
            filename: str
            type: enum CameraFileType
            file: CameraFile
            context: Context


        See also: gphoto2.gp_camera_folder_put_file
        """
        return _camera.Camera_folder_put_file(self, folder, filename, type, file, context)


    def folder_make_dir(self, folder: 'char const *', name: 'char const *', context: 'Context') -> "void":
        """
        folder_make_dir(self, folder, name, context)

        Parameters:
            folder: str
            name: str
            context: Context


        See also: gphoto2.gp_camera_folder_make_dir
        """
        return _camera.Camera_folder_make_dir(self, folder, name, context)


    def folder_remove_dir(self, folder: 'char const *', name: 'char const *', context: 'Context') -> "void":
        """
        folder_remove_dir(self, folder, name, context)

        Parameters:
            folder: str
            name: str
            context: Context


        See also: gphoto2.gp_camera_folder_remove_dir
        """
        return _camera.Camera_folder_remove_dir(self, folder, name, context)


    def file_get_info(self, folder: 'char const *', file: 'char const *', context: 'Context') -> "void":
        """
        file_get_info(self, folder, file, context)

        Parameters:
            folder: str
            file: str
            context: Context


        See also: gphoto2.gp_camera_file_get_info
        """
        return _camera.Camera_file_get_info(self, folder, file, context)


    def file_set_info(self, folder: 'char const *', file: 'char const *', info: 'CameraFileInfo', context: 'Context') -> "void":
        """
        file_set_info(self, folder, file, info, context)

        Parameters:
            folder: str
            file: str
            info: CameraFileInfo
            context: Context


        See also: gphoto2.gp_camera_file_set_info
        """
        return _camera.Camera_file_set_info(self, folder, file, info, context)


    def file_get(self, folder: 'char const *', file: 'char const *', type: 'CameraFileType', context: 'Context') -> "void":
        """
        file_get(self, folder, file, type, context)

        Parameters:
            folder: str
            file: str
            type: enum CameraFileType
            context: Context


        See also: gphoto2.gp_camera_file_get
        """
        return _camera.Camera_file_get(self, folder, file, type, context)


    def file_read(self, folder: 'char const *', file: 'char const *', type: 'CameraFileType', offset: 'uint64_t', buf: 'char *', context: 'Context') -> "void":
        """
        file_read(self, folder, file, type, offset, buf, context)

        Parameters:
            folder: str
            file: str
            type: enum CameraFileType
            offset: uint64_t
            buf: writable buffer
            context: Context


        See also: gphoto2.gp_camera_file_read
        """
        return _camera.Camera_file_read(self, folder, file, type, offset, buf, context)


    def file_delete(self, folder: 'char const *', file: 'char const *', context: 'Context') -> "void":
        """
        file_delete(self, folder, file, context)

        Parameters:
            folder: str
            file: str
            context: Context


        See also: gphoto2.gp_camera_file_delete
        """
        return _camera.Camera_file_delete(self, folder, file, context)

Camera.set_abilities = new_instancemethod(_camera.Camera_set_abilities, None, Camera)
Camera.get_abilities = new_instancemethod(_camera.Camera_get_abilities, None, Camera)
Camera.set_port_info = new_instancemethod(_camera.Camera_set_port_info, None, Camera)
Camera.get_port_info = new_instancemethod(_camera.Camera_get_port_info, None, Camera)
Camera.set_port_speed = new_instancemethod(_camera.Camera_set_port_speed, None, Camera)
Camera.get_port_speed = new_instancemethod(_camera.Camera_get_port_speed, None, Camera)
Camera.init = new_instancemethod(_camera.Camera_init, None, Camera)
Camera.exit = new_instancemethod(_camera.Camera_exit, None, Camera)
Camera.get_config = new_instancemethod(_camera.Camera_get_config, None, Camera)
Camera.set_config = new_instancemethod(_camera.Camera_set_config, None, Camera)
Camera.get_summary = new_instancemethod(_camera.Camera_get_summary, None, Camera)
Camera.get_manual = new_instancemethod(_camera.Camera_get_manual, None, Camera)
Camera.get_about = new_instancemethod(_camera.Camera_get_about, None, Camera)
Camera.capture = new_instancemethod(_camera.Camera_capture, None, Camera)
Camera.trigger_capture = new_instancemethod(_camera.Camera_trigger_capture, None, Camera)
Camera.capture_preview = new_instancemethod(_camera.Camera_capture_preview, None, Camera)
Camera.wait_for_event = new_instancemethod(_camera.Camera_wait_for_event, None, Camera)
Camera.get_storageinfo = new_instancemethod(_camera.Camera_get_storageinfo, None, Camera)
Camera.folder_list_files = new_instancemethod(_camera.Camera_folder_list_files, None, Camera)
Camera.folder_list_folders = new_instancemethod(_camera.Camera_folder_list_folders, None, Camera)
Camera.folder_delete_all = new_instancemethod(_camera.Camera_folder_delete_all, None, Camera)
Camera.folder_put_file = new_instancemethod(_camera.Camera_folder_put_file, None, Camera)
Camera.folder_make_dir = new_instancemethod(_camera.Camera_folder_make_dir, None, Camera)
Camera.folder_remove_dir = new_instancemethod(_camera.Camera_folder_remove_dir, None, Camera)
Camera.file_get_info = new_instancemethod(_camera.Camera_file_get_info, None, Camera)
Camera.file_set_info = new_instancemethod(_camera.Camera_file_set_info, None, Camera)
Camera.file_get = new_instancemethod(_camera.Camera_file_get, None, Camera)
Camera.file_read = new_instancemethod(_camera.Camera_file_read, None, Camera)
Camera.file_delete = new_instancemethod(_camera.Camera_file_delete, None, Camera)
Camera_swigregister = _camera.Camera_swigregister
Camera_swigregister(Camera)


def gp_camera_new() -> "Camera **":
    """
    gp_camera_new() -> int

    See also: gphoto2.Camera
    """
    return _camera.gp_camera_new()

def gp_camera_set_abilities(camera: 'Camera', abilities: 'CameraAbilities') -> "int":
    """
    gp_camera_set_abilities(camera, abilities) -> int

    Parameters:
        camera: Camera
        abilities: CameraAbilities


    See also: gphoto2.Camera.set_abilities
    """
    return _camera.gp_camera_set_abilities(camera, abilities)

def gp_camera_get_abilities(camera: 'Camera') -> "CameraAbilities *":
    """
    gp_camera_get_abilities(camera) -> int

    Parameters:
        camera: Camera


    See also: gphoto2.Camera.get_abilities
    """
    return _camera.gp_camera_get_abilities(camera)

def gp_camera_set_port_info(camera: 'Camera', info: '_GPPortInfo') -> "int":
    """
    gp_camera_set_port_info(camera, info) -> int

    Parameters:
        camera: Camera
        info: GPPortInfo


    See also: gphoto2.Camera.set_port_info
    """
    return _camera.gp_camera_set_port_info(camera, info)

def gp_camera_get_port_info(camera: 'Camera') -> "GPPortInfo *":
    """
    gp_camera_get_port_info(camera) -> int

    Parameters:
        camera: Camera


    See also: gphoto2.Camera.get_port_info
    """
    return _camera.gp_camera_get_port_info(camera)

def gp_camera_set_port_speed(camera: 'Camera', speed: 'int') -> "int":
    """
    gp_camera_set_port_speed(camera, speed) -> int

    Parameters:
        camera: Camera
        speed: int


    See also: gphoto2.Camera.set_port_speed
    """
    return _camera.gp_camera_set_port_speed(camera, speed)

def gp_camera_get_port_speed(camera: 'Camera') -> "int":
    """
    gp_camera_get_port_speed(camera) -> int

    Parameters:
        camera: Camera


    See also: gphoto2.Camera.get_port_speed
    """
    return _camera.gp_camera_get_port_speed(camera)

def gp_camera_autodetect(context: 'Context') -> "CameraList *":
    """
    gp_camera_autodetect(context) -> int

    Parameters:
        context: Context


    See also: gphoto2.Context.camera_autodetect
    """
    return _camera.gp_camera_autodetect(context)

def gp_camera_init(camera: 'Camera', context: 'Context') -> "int":
    """
    gp_camera_init(camera, context) -> int

    Parameters:
        camera: Camera
        context: Context


    See also: gphoto2.Camera.init
    """
    return _camera.gp_camera_init(camera, context)

def gp_camera_exit(camera: 'Camera', context: 'Context') -> "int":
    """
    gp_camera_exit(camera, context) -> int

    Parameters:
        camera: Camera
        context: Context


    See also: gphoto2.Camera.exit
    """
    return _camera.gp_camera_exit(camera, context)

def gp_camera_get_config(camera: 'Camera', context: 'Context') -> "CameraWidget **":
    """
    gp_camera_get_config(camera, context) -> int

    Parameters:
        camera: Camera
        context: Context


    See also: gphoto2.Camera.get_config
    """
    return _camera.gp_camera_get_config(camera, context)

def gp_camera_set_config(camera: 'Camera', window: 'CameraWidget', context: 'Context') -> "int":
    """
    gp_camera_set_config(camera, window, context) -> int

    Parameters:
        camera: Camera
        window: CameraWidget
        context: Context


    See also: gphoto2.Camera.set_config
    """
    return _camera.gp_camera_set_config(camera, window, context)

def gp_camera_get_summary(camera: 'Camera', context: 'Context') -> "CameraText *":
    """
    gp_camera_get_summary(camera, context) -> int

    Parameters:
        camera: Camera
        context: Context


    See also: gphoto2.Camera.get_summary
    """
    return _camera.gp_camera_get_summary(camera, context)

def gp_camera_get_manual(camera: 'Camera', context: 'Context') -> "CameraText *":
    """
    gp_camera_get_manual(camera, context) -> int

    Parameters:
        camera: Camera
        context: Context


    See also: gphoto2.Camera.get_manual
    """
    return _camera.gp_camera_get_manual(camera, context)

def gp_camera_get_about(camera: 'Camera', context: 'Context') -> "CameraText *":
    """
    gp_camera_get_about(camera, context) -> int

    Parameters:
        camera: Camera
        context: Context


    See also: gphoto2.Camera.get_about
    """
    return _camera.gp_camera_get_about(camera, context)

def gp_camera_capture(camera: 'Camera', type: 'CameraCaptureType', context: 'Context') -> "CameraFilePath *":
    """
    gp_camera_capture(camera, type, context) -> int

    Parameters:
        camera: Camera
        type: enum CameraCaptureType
        context: Context


    See also: gphoto2.Camera.capture
    """
    return _camera.gp_camera_capture(camera, type, context)

def gp_camera_trigger_capture(camera: 'Camera', context: 'Context') -> "int":
    """
    gp_camera_trigger_capture(camera, context) -> int

    Parameters:
        camera: Camera
        context: Context


    See also: gphoto2.Camera.trigger_capture
    """
    return _camera.gp_camera_trigger_capture(camera, context)

def gp_camera_wait_for_event(camera: 'Camera', timeout: 'int', context: 'Context') -> "void **":
    """
    gp_camera_wait_for_event(camera, timeout, context) -> int

    Parameters:
        camera: Camera
        timeout: int
        context: Context


    See also: gphoto2.Camera.wait_for_event
    """
    return _camera.gp_camera_wait_for_event(camera, timeout, context)

def gp_camera_get_storageinfo(camera: 'Camera', context: 'Context') -> "int *":
    """
    gp_camera_get_storageinfo(camera, context) -> int

    Parameters:
        camera: Camera
        context: Context


    See also: gphoto2.Camera.get_storageinfo
    """
    return _camera.gp_camera_get_storageinfo(camera, context)

def gp_camera_folder_list_files(camera: 'Camera', folder: 'char const *', context: 'Context') -> "CameraList *":
    """
    gp_camera_folder_list_files(camera, folder, context) -> int

    Parameters:
        camera: Camera
        folder: str
        context: Context


    See also: gphoto2.Camera.folder_list_files
    """
    return _camera.gp_camera_folder_list_files(camera, folder, context)

def gp_camera_folder_list_folders(camera: 'Camera', folder: 'char const *', context: 'Context') -> "CameraList *":
    """
    gp_camera_folder_list_folders(camera, folder, context) -> int

    Parameters:
        camera: Camera
        folder: str
        context: Context


    See also: gphoto2.Camera.folder_list_folders
    """
    return _camera.gp_camera_folder_list_folders(camera, folder, context)

def gp_camera_folder_delete_all(camera: 'Camera', folder: 'char const *', context: 'Context') -> "int":
    """
    gp_camera_folder_delete_all(camera, folder, context) -> int

    Parameters:
        camera: Camera
        folder: str
        context: Context


    See also: gphoto2.Camera.folder_delete_all
    """
    return _camera.gp_camera_folder_delete_all(camera, folder, context)

def gp_camera_folder_put_file(camera: 'Camera', folder: 'char const *', filename: 'char const *', type: 'CameraFileType', file: 'CameraFile', context: 'Context') -> "int":
    """
    gp_camera_folder_put_file(camera, folder, filename, type, file, context) -> int

    Parameters:
        camera: Camera
        folder: str
        filename: str
        type: enum CameraFileType
        file: CameraFile
        context: Context


    See also: gphoto2.Camera.folder_put_file
    """
    return _camera.gp_camera_folder_put_file(camera, folder, filename, type, file, context)

def gp_camera_folder_make_dir(camera: 'Camera', folder: 'char const *', name: 'char const *', context: 'Context') -> "int":
    """
    gp_camera_folder_make_dir(camera, folder, name, context) -> int

    Parameters:
        camera: Camera
        folder: str
        name: str
        context: Context


    See also: gphoto2.Camera.folder_make_dir
    """
    return _camera.gp_camera_folder_make_dir(camera, folder, name, context)

def gp_camera_folder_remove_dir(camera: 'Camera', folder: 'char const *', name: 'char const *', context: 'Context') -> "int":
    """
    gp_camera_folder_remove_dir(camera, folder, name, context) -> int

    Parameters:
        camera: Camera
        folder: str
        name: str
        context: Context


    See also: gphoto2.Camera.folder_remove_dir
    """
    return _camera.gp_camera_folder_remove_dir(camera, folder, name, context)

def gp_camera_file_get_info(camera: 'Camera', folder: 'char const *', file: 'char const *', context: 'Context') -> "CameraFileInfo *":
    """
    gp_camera_file_get_info(camera, folder, file, context) -> int

    Parameters:
        camera: Camera
        folder: str
        file: str
        context: Context


    See also: gphoto2.Camera.file_get_info
    """
    return _camera.gp_camera_file_get_info(camera, folder, file, context)

def gp_camera_file_set_info(camera: 'Camera', folder: 'char const *', file: 'char const *', info: 'CameraFileInfo', context: 'Context') -> "int":
    """
    gp_camera_file_set_info(camera, folder, file, info, context) -> int

    Parameters:
        camera: Camera
        folder: str
        file: str
        info: CameraFileInfo
        context: Context


    See also: gphoto2.Camera.file_set_info
    """
    return _camera.gp_camera_file_set_info(camera, folder, file, info, context)

def gp_camera_file_get(camera: 'Camera', folder: 'char const *', file: 'char const *', type: 'CameraFileType', context: 'Context') -> "CameraFile *":
    """
    gp_camera_file_get(camera, folder, file, type, context) -> int

    Parameters:
        camera: Camera
        folder: str
        file: str
        type: enum CameraFileType
        context: Context


    See also: gphoto2.Camera.file_get
    """
    return _camera.gp_camera_file_get(camera, folder, file, type, context)

def gp_camera_file_read(camera: 'Camera', folder: 'char const *', file: 'char const *', type: 'CameraFileType', offset: 'uint64_t', buf: 'char *', context: 'Context') -> "uint64_t *":
    """
    gp_camera_file_read(camera, folder, file, type, offset, buf, context) -> int

    Parameters:
        camera: Camera
        folder: str
        file: str
        type: enum CameraFileType
        offset: uint64_t
        buf: writable buffer
        context: Context


    See also: gphoto2.Camera.file_read
    """
    return _camera.gp_camera_file_read(camera, folder, file, type, offset, buf, context)

def gp_camera_file_delete(camera: 'Camera', folder: 'char const *', file: 'char const *', context: 'Context') -> "int":
    """
    gp_camera_file_delete(camera, folder, file, context) -> int

    Parameters:
        camera: Camera
        folder: str
        file: str
        context: Context


    See also: gphoto2.Camera.file_delete
    """
    return _camera.gp_camera_file_delete(camera, folder, file, context)

def gp_camera_set_timeout_funcs(camera: 'Camera', start_func: 'CameraTimeoutStartFunc', stop_func: 'CameraTimeoutStopFunc', data: 'void *') -> "void":
    """
    gp_camera_set_timeout_funcs(camera, start_func, stop_func, data)

    Parameters:
        camera: Camera
        start_func: CameraTimeoutStartFunc
        stop_func: CameraTimeoutStopFunc
        data: object

    """
    return _camera.gp_camera_set_timeout_funcs(camera, start_func, stop_func, data)

def gp_camera_start_timeout(camera: 'Camera', timeout: 'unsigned int', func: 'CameraTimeoutFunc') -> "int":
    """
    gp_camera_start_timeout(camera, timeout, func) -> int

    Parameters:
        camera: Camera
        timeout: unsigned int
        func: CameraTimeoutFunc

    """
    return _camera.gp_camera_start_timeout(camera, timeout, func)

def gp_camera_stop_timeout(camera: 'Camera', id: 'unsigned int') -> "void":
    """
    gp_camera_stop_timeout(camera, id)

    Parameters:
        camera: Camera
        id: unsigned int

    """
    return _camera.gp_camera_stop_timeout(camera, id)


