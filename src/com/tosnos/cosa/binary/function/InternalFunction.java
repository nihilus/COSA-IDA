package com.tosnos.cosa.binary.function;

import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.asm.*;
import com.tosnos.cosa.binary.asm.value.*;
import com.tosnos.cosa.binary.asm.value.memory.*;
import com.tosnos.cosa.binary.function.formatter.Formatter;
import com.tosnos.cosa.util.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Type;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by kevin on 5/27/14.
 */
public class InternalFunction extends Function {
    private static Immediate ANDROID_BITMAP_RESULT_SUCCESS = Immediate.ZERO;
    private static AbstractMemory functions =  new AbstractMemory("lib");
    private static Map<String, Address> functionAddressMap = new HashMap<String, Address>();


    public static final AbstractValue pid_t = new Pthread();
    public static final InternalFunction EMPTY_VOID = new InternalFunction("empty", new Type[]{}, VOID);
    public static final InternalFunction EMPTY_INT = new InternalFunction("empty", new Type[]{}, INT);

    public static final InternalFunction hw_get_module = new InternalFunction("hw_get_module", new Type[]{INT, INT}, NEW);
    public static final InternalFunction calloc = new InternalFunction("calloc", new Type[]{INT, INT}, NEW);
    public static final InternalFunction malloc = new InternalFunction("malloc", new Type[]{INT}, NEW);
    public static final InternalFunction realloc = new InternalFunction("realloc", new Type[]{ADDRESS, INT}, NEW);
    public static final InternalFunction _Znwj = new InternalFunction("_Znwj", new Type[]{INT}, NEW);
    public static final InternalFunction _Znaj = new InternalFunction("_Znaj", new Type[]{INT}, NEW);
    public static final InternalFunction free = new InternalFunction("free", new Type[]{VOID}, VOID);
    public static final InternalFunction _ZdaPv = new InternalFunction("_ZdaPv", new Type[]{VOID}, VOID);
    public static final InternalFunction _ZdlPv = new InternalFunction("_ZdlPv", new Type[]{VOID}, VOID);
    public static final InternalFunction printf = new InternalFunction("printf", new Type[]{FORMAT}, INT);
    public static final InternalFunction snprintf = new InternalFunction("snprintf", new Type[]{ADDRESS, INT, FORMAT}, INT);
    public static final InternalFunction asprintf = new InternalFunction("asprintf", new Type[]{ADDRESS, FORMAT}, INT);
    public static final InternalFunction sprintf = new InternalFunction("sprintf", new Type[]{ADDRESS, FORMAT}, INT);
    public static final InternalFunction __aeabi_d2uiz = new InternalFunction("__aeabi_d2uiz", new Type[]{}, ADDRESS);
    public static final InternalFunction __aeabi_memcpy = new InternalFunction("__aeabi_memcpy", new Type[]{ADDRESS, ADDRESS, INT}, ADDRESS);
    public static final InternalFunction memcpy = new InternalFunction("memcpy", new Type[]{ADDRESS, ADDRESS, INT}, ADDRESS);
    public static final InternalFunction setlocale = new InternalFunction("setlocale", new Type[]{INT, STRING}, STRING);
    public static final InternalFunction __aeabi_ul2d = new InternalFunction("__aeabi_ul2d", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_memset = new InternalFunction("__aeabi_memset", new Type[]{ADDRESS, INT, INT}, VOID);
    public static final InternalFunction memset = new InternalFunction("memset", new Type[]{ADDRESS, INT, INT}, ADDRESS);
    public static final InternalFunction pthread_getspecific = new InternalFunction("pthread_getspecific", new Type[]{ADDRESS}, ADDRESS);
    public static final InternalFunction getpid = new InternalFunction("getpid", new Type[]{}, INT);
    public static final InternalFunction strlen = new InternalFunction("strlen", new Type[]{STRING}, INT);
    public static final InternalFunction strdup = new InternalFunction("strdup", new Type[]{STRING}, NEW);
    public static final InternalFunction strstr = new InternalFunction("strstr", new Type[]{STRING, STRING}, ADDRESS);
    public static final InternalFunction strcpy = new InternalFunction("strcpy", new Type[]{STRING, STRING}, ADDRESS);
    public static final InternalFunction strcmp = new InternalFunction("strcmp", new Type[]{STRING, STRING}, INT);
    public static final InternalFunction strcasecmp = new InternalFunction("strcasecmp", new Type[]{STRING, STRING}, INT);
    public static final InternalFunction memmove = new InternalFunction("memmove", new Type[]{ADDRESS, ADDRESS, INT}, ADDRESS);
    public static final InternalFunction pthread_mutex_lock = new InternalFunction("pthread_mutex_lock", new Type[]{VOID}, INT);
    public static final InternalFunction pthread_mutex_unlock = new InternalFunction("pthread_mutex_unlock", new Type[]{VOID}, INT);
    public static final InternalFunction pthread_once = new InternalFunction("pthread_once", new Type[]{VOID, VOID}, INT);
    public static final InternalFunction lrand48 = new InternalFunction("lrand48", new Type[]{}, DOUBLE);
    public static final InternalFunction sigaction = new InternalFunction("sigaction", new Type[]{INT, VOID, VOID}, INT);
    public static final InternalFunction __errno = new InternalFunction("__errno", new Type[]{}, INT);
    public static final InternalFunction strerror_r = new InternalFunction("strerror_r", new Type[]{INT}, VOID);
    public static final InternalFunction perror = new InternalFunction("perror", new Type[]{VOID}, VOID);
    public static final InternalFunction ioctl = new InternalFunction("ioctl", new Type[]{INT, INT}, INT, Kind.RESTRICT);
    public static final InternalFunction close = new InternalFunction("close", new Type[]{INT}, INT, Kind.RESTRICT);
    public static final InternalFunction open = new InternalFunction("open", new Type[]{STRING, INT, INT}, INT, Kind.RESTRICT);
    public static final InternalFunction fopen = new InternalFunction("fopen", new Type[]{STRING, VOID}, INT, Kind.RESTRICT);
    public static final InternalFunction fclose = new InternalFunction("fclose", new Type[]{INT}, INT, Kind.RESTRICT);
    public static final InternalFunction setuid = new InternalFunction("setuid", new Type[]{INT}, INT);
    public static final InternalFunction fwrite = new InternalFunction("fwrite", new Type[]{VOID, INT, INT, INT}, INT, Kind.RESTRICT);
    public static final InternalFunction dlopen = new InternalFunction("dlopen", new Type[]{STRING, INT}, INT); // todo: check it later
    public static final InternalFunction dlsym = new InternalFunction("dlsym", new Type[]{VOID, STRING}, INT);
    public static final InternalFunction dlclose = new InternalFunction("dlclose", new Type[]{VOID}, INT);
    public static final InternalFunction abort = new InternalFunction("abort", new Type[]{}, VOID, Kind.EXIT);
    public static final InternalFunction __stack_chk_fail = new InternalFunction("__stack_chk_fail", new Type[]{}, VOID, Kind.EXIT);
    public static final InternalFunction AAssetDir_close = new InternalFunction("AAssetDir_close", new Type[]{}, VOID);
    public static final InternalFunction AAssetDir_getNextFileName = new InternalFunction("AAssetDir_getNextFileName", new Type[]{}, VOID);
    public static final InternalFunction AAssetManager_fromJava = new InternalFunction("AAssetManager_fromJava", new Type[]{}, VOID);
    public static final InternalFunction AAssetManager_open = new InternalFunction("AAssetManager_open", new Type[]{}, VOID);
    public static final InternalFunction AAssetManager_openDir = new InternalFunction("AAssetManager_openDir", new Type[]{}, VOID);
    public static final InternalFunction AAsset_close = new InternalFunction("AAsset_close", new Type[]{}, VOID);
    public static final InternalFunction AAsset_getBuffer = new InternalFunction("AAsset_getBuffer", new Type[]{}, VOID);
    public static final InternalFunction AAsset_getLength = new InternalFunction("AAsset_getLength", new Type[]{}, VOID);
    public static final InternalFunction AAsset_getRemainingLength = new InternalFunction("AAsset_getRemainingLength", new Type[]{}, VOID);
    public static final InternalFunction AAsset_openFileDescriptor = new InternalFunction("AAsset_openFileDescriptor", new Type[]{}, VOID);
    public static final InternalFunction AAsset_read = new InternalFunction("AAsset_read", new Type[]{}, VOID);
    public static final InternalFunction AAsset_seek = new InternalFunction("AAsset_seek", new Type[]{}, VOID);
    public static final InternalFunction AConfiguration_delete = new InternalFunction("AConfiguration_delete", new Type[]{}, VOID);
    public static final InternalFunction AConfiguration_fromAssetManager = new InternalFunction("AConfiguration_fromAssetManager", new Type[]{}, VOID);
    public static final InternalFunction AConfiguration_getCountry = new InternalFunction("AConfiguration_getCountry", new Type[]{}, VOID);
    public static final InternalFunction AConfiguration_getLanguage = new InternalFunction("AConfiguration_getLanguage", new Type[]{}, VOID);
    public static final InternalFunction AConfiguration_new = new InternalFunction("AConfiguration_new", new Type[]{}, VOID);
    public static final InternalFunction AES_cbc_encrypt = new InternalFunction("AES_cbc_encrypt", new Type[]{}, VOID);
    public static final InternalFunction AES_decrypt = new InternalFunction("AES_decrypt", new Type[]{}, VOID);
    public static final InternalFunction AES_encrypt = new InternalFunction("AES_encrypt", new Type[]{}, VOID);
    public static final InternalFunction AES_set_decrypt_key = new InternalFunction("AES_set_decrypt_key", new Type[]{}, VOID);
    public static final InternalFunction AES_set_encrypt_key = new InternalFunction("AES_set_encrypt_key", new Type[]{}, VOID);
    public static final InternalFunction AInputEvent_getDeviceId = new InternalFunction("AInputEvent_getDeviceId", new Type[]{}, VOID);
    public static final InternalFunction AInputEvent_getSource = new InternalFunction("AInputEvent_getSource", new Type[]{}, VOID);
    public static final InternalFunction AInputEvent_getType = new InternalFunction("AInputEvent_getType", new Type[]{}, VOID);
    public static final InternalFunction AInputQueue_attachLooper = new InternalFunction("AInputQueue_attachLooper", new Type[]{}, VOID);
    public static final InternalFunction AInputQueue_detachLooper = new InternalFunction("AInputQueue_detachLooper", new Type[]{}, VOID);
    public static final InternalFunction AInputQueue_finishEvent = new InternalFunction("AInputQueue_finishEvent", new Type[]{}, VOID);
    public static final InternalFunction AInputQueue_getEvent = new InternalFunction("AInputQueue_getEvent", new Type[]{}, VOID);
    public static final InternalFunction AInputQueue_preDispatchEvent = new InternalFunction("AInputQueue_preDispatchEvent", new Type[]{}, VOID);
    public static final InternalFunction AKeyEvent_getAction = new InternalFunction("AKeyEvent_getAction", new Type[]{}, VOID);
    public static final InternalFunction AKeyEvent_getDownTime = new InternalFunction("AKeyEvent_getDownTime", new Type[]{}, VOID);
    public static final InternalFunction AKeyEvent_getEventTime = new InternalFunction("AKeyEvent_getEventTime", new Type[]{}, VOID);
    public static final InternalFunction AKeyEvent_getFlags = new InternalFunction("AKeyEvent_getFlags", new Type[]{}, VOID);
    public static final InternalFunction AKeyEvent_getKeyCode = new InternalFunction("AKeyEvent_getKeyCode", new Type[]{}, VOID);
    public static final InternalFunction AKeyEvent_getMetaState = new InternalFunction("AKeyEvent_getMetaState", new Type[]{}, VOID);
    public static final InternalFunction AKeyEvent_getRepeatCount = new InternalFunction("AKeyEvent_getRepeatCount", new Type[]{}, VOID);
    public static final InternalFunction AKeyEvent_getScanCode = new InternalFunction("AKeyEvent_getScanCode", new Type[]{}, VOID);
    public static final InternalFunction ALooper_acquire = new InternalFunction("ALooper_acquire", new Type[]{}, VOID);
    public static final InternalFunction ALooper_addFd = new InternalFunction("ALooper_addFd", new Type[]{}, VOID);
    public static final InternalFunction ALooper_forThread = new InternalFunction("ALooper_forThread", new Type[]{}, VOID);
    public static final InternalFunction ALooper_pollAll = new InternalFunction("ALooper_pollAll", new Type[]{}, VOID);
    public static final InternalFunction ALooper_prepare = new InternalFunction("ALooper_prepare", new Type[]{}, VOID);
    public static final InternalFunction ALooper_release = new InternalFunction("ALooper_release", new Type[]{}, VOID);
    public static final InternalFunction ALooper_removeFd = new InternalFunction("ALooper_removeFd", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getAction = new InternalFunction("AMotionEvent_getAction", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getDownTime = new InternalFunction("AMotionEvent_getDownTime", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getEdgeFlags = new InternalFunction("AMotionEvent_getEdgeFlags", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getEventTime = new InternalFunction("AMotionEvent_getEventTime", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getFlags = new InternalFunction("AMotionEvent_getFlags", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getHistoricalEventTime = new InternalFunction("AMotionEvent_getHistoricalEventTime", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getHistoricalPressure = new InternalFunction("AMotionEvent_getHistoricalPressure", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getHistoricalSize = new InternalFunction("AMotionEvent_getHistoricalSize", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getHistoricalX = new InternalFunction("AMotionEvent_getHistoricalX", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getHistoricalY = new InternalFunction("AMotionEvent_getHistoricalY", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getHistorySize = new InternalFunction("AMotionEvent_getHistorySize", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getMetaState = new InternalFunction("AMotionEvent_getMetaState", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getOrientation = new InternalFunction("AMotionEvent_getOrientation", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getPointerCount = new InternalFunction("AMotionEvent_getPointerCount", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getPointerId = new InternalFunction("AMotionEvent_getPointerId", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getPressure = new InternalFunction("AMotionEvent_getPressure", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getSize = new InternalFunction("AMotionEvent_getSize", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getToolMajor = new InternalFunction("AMotionEvent_getToolMajor", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getToolMinor = new InternalFunction("AMotionEvent_getToolMinor", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getTouchMajor = new InternalFunction("AMotionEvent_getTouchMajor", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getTouchMinor = new InternalFunction("AMotionEvent_getTouchMinor", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getX = new InternalFunction("AMotionEvent_getX", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getXPrecision = new InternalFunction("AMotionEvent_getXPrecision", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getY = new InternalFunction("AMotionEvent_getY", new Type[]{}, VOID);
    public static final InternalFunction AMotionEvent_getYPrecision = new InternalFunction("AMotionEvent_getYPrecision", new Type[]{}, VOID);
    public static final InternalFunction ANativeActivity_finish = new InternalFunction("ANativeActivity_finish", new Type[]{}, VOID);
    public static final InternalFunction ANativeWindow_acquire = new InternalFunction("ANativeWindow_acquire", new Type[]{}, VOID);
    public static final InternalFunction ANativeWindow_fromSurface = new InternalFunction("ANativeWindow_fromSurface", new Type[]{}, VOID);
    public static final InternalFunction ANativeWindow_getFormat = new InternalFunction("ANativeWindow_getFormat", new Type[]{}, VOID);
    public static final InternalFunction ANativeWindow_getHeight = new InternalFunction("ANativeWindow_getHeight", new Type[]{}, VOID);
    public static final InternalFunction ANativeWindow_getWidth = new InternalFunction("ANativeWindow_getWidth", new Type[]{}, VOID);
    public static final InternalFunction ANativeWindow_lock = new InternalFunction("ANativeWindow_lock", new Type[]{}, VOID);
    public static final InternalFunction ANativeWindow_release = new InternalFunction("ANativeWindow_release", new Type[]{}, VOID);
    public static final InternalFunction ANativeWindow_setBuffersGeometry = new InternalFunction("ANativeWindow_setBuffersGeometry", new Type[]{}, VOID);
    public static final InternalFunction ANativeWindow_unlockAndPost = new InternalFunction("ANativeWindow_unlockAndPost", new Type[]{}, VOID);
    public static final InternalFunction ASN1_INTEGER_get = new InternalFunction("ASN1_INTEGER_get", new Type[]{}, VOID);
    public static final InternalFunction ASensorEventQueue_disableSensor = new InternalFunction("ASensorEventQueue_disableSensor", new Type[]{}, VOID);
    public static final InternalFunction ASensorEventQueue_enableSensor = new InternalFunction("ASensorEventQueue_enableSensor", new Type[]{}, VOID);
    public static final InternalFunction ASensorEventQueue_getEvents = new InternalFunction("ASensorEventQueue_getEvents", new Type[]{}, VOID);
    public static final InternalFunction ASensorEventQueue_hasEvents = new InternalFunction("ASensorEventQueue_hasEvents", new Type[]{}, VOID);
    public static final InternalFunction ASensorEventQueue_setEventRate = new InternalFunction("ASensorEventQueue_setEventRate", new Type[]{}, VOID);
    public static final InternalFunction ASensorManager_createEventQueue = new InternalFunction("ASensorManager_createEventQueue", new Type[]{}, VOID);
    public static final InternalFunction ASensorManager_destroyEventQueue = new InternalFunction("ASensorManager_destroyEventQueue", new Type[]{}, VOID);
    public static final InternalFunction ASensorManager_getDefaultSensor = new InternalFunction("ASensorManager_getDefaultSensor", new Type[]{}, VOID);
    public static final InternalFunction ASensorManager_getInstance = new InternalFunction("ASensorManager_getInstance", new Type[]{}, VOID);
    public static final InternalFunction ASensorManager_getSensorList = new InternalFunction("ASensorManager_getSensorList", new Type[]{}, VOID);
    public static final InternalFunction ASensor_getMinDelay = new InternalFunction("ASensor_getMinDelay", new Type[]{}, VOID);
    public static final InternalFunction ASensor_getName = new InternalFunction("ASensor_getName", new Type[]{}, VOID);
    public static final InternalFunction ASensor_getResolution = new InternalFunction("ASensor_getResolution", new Type[]{}, VOID);
    public static final InternalFunction ASensor_getType = new InternalFunction("ASensor_getType", new Type[]{}, VOID);
    public static final InternalFunction ASensor_getVendor = new InternalFunction("ASensor_getVendor", new Type[]{}, VOID);
    public static final InternalFunction AndroidBitmap_getInfo = new InternalFunction("AndroidBitmap_getInfo", new Type[]{JNIENV, JOBJECT, ADDRESS}, INT);
    public static final InternalFunction AndroidBitmap_lockPixels = new InternalFunction("AndroidBitmap_lockPixels", new Type[]{}, VOID);
    public static final InternalFunction AndroidBitmap_unlockPixels = new InternalFunction("AndroidBitmap_unlockPixels", new Type[]{}, VOID);
    public static final InternalFunction BIO_ctrl = new InternalFunction("BIO_ctrl", new Type[]{}, VOID);
    public static final InternalFunction BIO_f_base64 = new InternalFunction("BIO_f_base64", new Type[]{}, VOID);
    public static final InternalFunction BIO_free = new InternalFunction("BIO_free", new Type[]{}, VOID);
    public static final InternalFunction BIO_free_all = new InternalFunction("BIO_free_all", new Type[]{}, VOID);
    public static final InternalFunction BIO_gets = new InternalFunction("BIO_gets", new Type[]{}, VOID);
    public static final InternalFunction BIO_new = new InternalFunction("BIO_new", new Type[]{}, VOID);
    public static final InternalFunction BIO_new_file = new InternalFunction("BIO_new_file", new Type[]{}, VOID);
    public static final InternalFunction BIO_new_mem_buf = new InternalFunction("BIO_new_mem_buf", new Type[]{}, VOID);
    public static final InternalFunction BIO_new_socket = new InternalFunction("BIO_new_socket", new Type[]{}, VOID);
    public static final InternalFunction BIO_new_ssl_connect = new InternalFunction("BIO_new_ssl_connect", new Type[]{}, VOID);
    public static final InternalFunction BIO_push = new InternalFunction("BIO_push", new Type[]{}, VOID);
    public static final InternalFunction BIO_read = new InternalFunction("BIO_read", new Type[]{}, VOID);
    public static final InternalFunction BIO_s_mem = new InternalFunction("BIO_s_mem", new Type[]{}, VOID);
    public static final InternalFunction BIO_set_flags = new InternalFunction("BIO_set_flags", new Type[]{}, VOID);
    public static final InternalFunction BIO_write = new InternalFunction("BIO_write", new Type[]{}, VOID);
    public static final InternalFunction BN_CTX_free = new InternalFunction("BN_CTX_free", new Type[]{}, VOID);
    public static final InternalFunction BN_CTX_new = new InternalFunction("BN_CTX_new", new Type[]{}, VOID);
    public static final InternalFunction BN_bin2bn = new InternalFunction("BN_bin2bn", new Type[]{}, VOID);
    public static final InternalFunction BN_bn2bin = new InternalFunction("BN_bn2bin", new Type[]{}, VOID);
    public static final InternalFunction BN_bn2hex = new InternalFunction("BN_bn2hex", new Type[]{}, VOID);
    public static final InternalFunction BN_cmp = new InternalFunction("BN_cmp", new Type[]{}, VOID);
    public static final InternalFunction BN_copy = new InternalFunction("BN_copy", new Type[]{}, VOID);
    public static final InternalFunction BN_free = new InternalFunction("BN_free", new Type[]{}, VOID);
    public static final InternalFunction BN_hex2bn = new InternalFunction("BN_hex2bn", new Type[]{}, VOID);
    public static final InternalFunction BN_mod_exp = new InternalFunction("BN_mod_exp", new Type[]{}, VOID);
    public static final InternalFunction BN_new = new InternalFunction("BN_new", new Type[]{}, VOID);
    public static final InternalFunction BN_num_bits = new InternalFunction("BN_num_bits", new Type[]{}, VOID);
    public static final InternalFunction BN_set_word = new InternalFunction("BN_set_word", new Type[]{}, VOID);
    public static final InternalFunction BN_sub_word = new InternalFunction("BN_sub_word", new Type[]{}, VOID);
    public static final InternalFunction BN_value_one = new InternalFunction("BN_value_one", new Type[]{}, VOID);
    public static final InternalFunction CRYPTO_cleanup_all_ex_data = new InternalFunction("CRYPTO_cleanup_all_ex_data", new Type[]{}, VOID);
    public static final InternalFunction CRYPTO_free = new InternalFunction("CRYPTO_free", new Type[]{}, VOID);
    public static final InternalFunction CRYPTO_get_locking_callback = new InternalFunction("CRYPTO_get_locking_callback", new Type[]{}, VOID);
    public static final InternalFunction CRYPTO_num_locks = new InternalFunction("CRYPTO_num_locks", new Type[]{}, VOID);
    public static final InternalFunction CRYPTO_set_locking_callback = new InternalFunction("CRYPTO_set_locking_callback", new Type[]{}, VOID);
    public static final InternalFunction DES_ecb3_encrypt = new InternalFunction("DES_ecb3_encrypt", new Type[]{}, VOID);
    public static final InternalFunction DES_set_key = new InternalFunction("DES_set_key", new Type[]{}, VOID);
    public static final InternalFunction DH_compute_key = new InternalFunction("DH_compute_key", new Type[]{}, VOID);
    public static final InternalFunction DH_free = new InternalFunction("DH_free", new Type[]{}, VOID);
    public static final InternalFunction DH_generate_key = new InternalFunction("DH_generate_key", new Type[]{}, VOID);
    public static final InternalFunction DH_new = new InternalFunction("DH_new", new Type[]{}, VOID);
    public static final InternalFunction ERR_error_string = new InternalFunction("ERR_error_string", new Type[]{}, VOID);
    public static final InternalFunction ERR_error_string_n = new InternalFunction("ERR_error_string_n", new Type[]{}, VOID);
    public static final InternalFunction ERR_free_strings = new InternalFunction("ERR_free_strings", new Type[]{}, VOID);
    public static final InternalFunction ERR_get_error = new InternalFunction("ERR_get_error", new Type[]{}, VOID);
    public static final InternalFunction ERR_peek_last_error = new InternalFunction("ERR_peek_last_error", new Type[]{}, VOID);
    public static final InternalFunction ERR_print_errors_fp = new InternalFunction("ERR_print_errors_fp", new Type[]{}, VOID);
    public static final InternalFunction ERR_remove_state = new InternalFunction("ERR_remove_state", new Type[]{}, VOID);
    public static final InternalFunction EVP_aes_256_cbc = new InternalFunction("EVP_aes_256_cbc", new Type[]{}, VOID);
    public static final InternalFunction EVP_BytesToKey = new InternalFunction("EVP_BytesToKey", new Type[]{}, VOID);
    public static final InternalFunction EVP_CIPHER_CTX_cleanup = new InternalFunction("EVP_CIPHER_CTX_cleanup", new Type[]{}, VOID);
    public static final InternalFunction EVP_CIPHER_CTX_init = new InternalFunction("EVP_CIPHER_CTX_init", new Type[]{}, VOID);
    public static final InternalFunction EVP_CIPHER_CTX_set_padding = new InternalFunction("EVP_CIPHER_CTX_set_padding", new Type[]{}, VOID);
    public static final InternalFunction EVP_CIPHER_block_size = new InternalFunction("EVP_CIPHER_block_size", new Type[]{}, VOID);
    public static final InternalFunction EVP_CIPHER_iv_length = new InternalFunction("EVP_CIPHER_iv_length", new Type[]{}, VOID);
    public static final InternalFunction EVP_CIPHER_key_length = new InternalFunction("EVP_CIPHER_key_length", new Type[]{}, VOID);
    public static final InternalFunction EVP_CIPHER_nid = new InternalFunction("EVP_CIPHER_nid", new Type[]{}, VOID);
    public static final InternalFunction EVP_CipherFinal = new InternalFunction("EVP_CipherFinal", new Type[]{}, VOID);
    public static final InternalFunction EVP_CipherInit = new InternalFunction("EVP_CipherInit", new Type[]{}, VOID);
    public static final InternalFunction EVP_CipherUpdate = new InternalFunction("EVP_CipherUpdate", new Type[]{}, VOID);
    public static final InternalFunction EVP_DecryptFinal_ex = new InternalFunction("EVP_DecryptFinal_ex", new Type[]{}, VOID);
    public static final InternalFunction EVP_DecryptInit = new InternalFunction("EVP_DecryptInit", new Type[]{}, VOID);
    public static final InternalFunction EVP_DecryptInit_ex = new InternalFunction("EVP_DecryptInit_ex", new Type[]{}, VOID);
    public static final InternalFunction EVP_DecryptUpdate = new InternalFunction("EVP_DecryptUpdate", new Type[]{}, VOID);
    public static final InternalFunction EVP_EncryptFinal = new InternalFunction("EVP_EncryptFinal", new Type[]{}, VOID);
    public static final InternalFunction EVP_EncryptFinal_ex = new InternalFunction("EVP_EncryptFinal_ex", new Type[]{}, VOID);
    public static final InternalFunction EVP_EncryptInit = new InternalFunction("EVP_EncryptInit", new Type[]{}, VOID);
    public static final InternalFunction EVP_EncryptInit_ex = new InternalFunction("EVP_EncryptInit_ex", new Type[]{}, VOID);
    public static final InternalFunction EVP_EncryptUpdate = new InternalFunction("EVP_EncryptUpdate", new Type[]{}, VOID);
    public static final InternalFunction EVP_MD_size = new InternalFunction("EVP_MD_size", new Type[]{}, VOID);
    public static final InternalFunction EVP_PKEY_bits = new InternalFunction("EVP_PKEY_bits", new Type[]{}, VOID);
    public static final InternalFunction EVP_PKEY_free = new InternalFunction("EVP_PKEY_free", new Type[]{}, VOID);
    public static final InternalFunction EVP_PKEY_get1_RSA = new InternalFunction("EVP_PKEY_get1_RSA", new Type[]{}, VOID);
    public static final InternalFunction EVP_PKEY_size = new InternalFunction("EVP_PKEY_size", new Type[]{}, VOID);
    public static final InternalFunction EVP_aes_128_ecb = new InternalFunction("EVP_aes_128_ecb", new Type[]{}, VOID);
    public static final InternalFunction EVP_cleanup = new InternalFunction("EVP_cleanup", new Type[]{}, VOID);
    public static final InternalFunction EVP_des_cbc = new InternalFunction("EVP_des_cbc", new Type[]{}, VOID);
    public static final InternalFunction EVP_des_ede3_cbc = new InternalFunction("EVP_des_ede3_cbc", new Type[]{}, VOID);
    public static final InternalFunction EVP_get_cipherbyname = new InternalFunction("EVP_get_cipherbyname", new Type[]{}, VOID);
    public static final InternalFunction EVP_rc4 = new InternalFunction("EVP_rc4", new Type[]{}, VOID);
    public static final InternalFunction EVP_sha1 = new InternalFunction("EVP_sha1", new Type[]{}, VOID);
    public static final InternalFunction HMAC_CTX_cleanup = new InternalFunction("HMAC_CTX_cleanup", new Type[]{}, VOID);
    public static final InternalFunction HMAC_CTX_init = new InternalFunction("HMAC_CTX_init", new Type[]{}, VOID);
    public static final InternalFunction HMAC_Final = new InternalFunction("HMAC_Final", new Type[]{}, VOID);
    public static final InternalFunction HMAC_Init_ex = new InternalFunction("HMAC_Init_ex", new Type[]{}, VOID);
    public static final InternalFunction HMAC_Update = new InternalFunction("HMAC_Update", new Type[]{}, VOID);
    public static final InternalFunction MD5_Final = new InternalFunction("MD5_Final", new Type[]{}, VOID);
    public static final InternalFunction MD5_Init = new InternalFunction("MD5_Init", new Type[]{}, VOID);
    public static final InternalFunction MD5_Update = new InternalFunction("MD5_Update", new Type[]{}, VOID);
    public static final InternalFunction OBJ_nid2sn = new InternalFunction("OBJ_nid2sn", new Type[]{}, VOID);
    public static final InternalFunction OPENSSL_add_all_algorithms_noconf = new InternalFunction("OPENSSL_add_all_algorithms_noconf", new Type[]{}, VOID);
    public static final InternalFunction PKCS5_PBKDF2_HMAC_SHA1 = new InternalFunction("PKCS5_PBKDF2_HMAC_SHA1", new Type[]{}, VOID);
    public static final InternalFunction PKCS7_free = new InternalFunction("PKCS7_free", new Type[]{}, VOID);
    public static final InternalFunction RAND_add = new InternalFunction("RAND_add", new Type[]{}, VOID);
    public static final InternalFunction RAND_bytes = new InternalFunction("RAND_bytes", new Type[]{}, VOID);
    public static final InternalFunction RSA_free = new InternalFunction("RSA_free", new Type[]{}, VOID);
    public static final InternalFunction RSA_generate_key_ex = new InternalFunction("RSA_generate_key_ex", new Type[]{}, VOID);
    public static final InternalFunction RSA_new = new InternalFunction("RSA_new", new Type[]{}, VOID);
    public static final InternalFunction RSA_private_decrypt = new InternalFunction("RSA_private_decrypt", new Type[]{}, VOID);
    public static final InternalFunction RSA_public_encrypt = new InternalFunction("RSA_public_encrypt", new Type[]{}, VOID);
    public static final InternalFunction RSA_sign = new InternalFunction("RSA_sign", new Type[]{}, VOID);
    public static final InternalFunction RSA_size = new InternalFunction("RSA_size", new Type[]{}, VOID);
    public static final InternalFunction RSA_verify = new InternalFunction("RSA_verify", new Type[]{}, VOID);
    public static final InternalFunction SHA1 = new InternalFunction("SHA1", new Type[]{}, VOID);
    public static final InternalFunction SHA1Final = new InternalFunction("SHA1Final", new Type[]{}, VOID);
    public static final InternalFunction SHA1Init = new InternalFunction("SHA1Init", new Type[]{}, VOID);
    public static final InternalFunction SHA1Update = new InternalFunction("SHA1Update", new Type[]{}, VOID);
    public static final InternalFunction SHA1_Final = new InternalFunction("SHA1_Final", new Type[]{}, VOID);
    public static final InternalFunction SHA1_Init = new InternalFunction("SHA1_Init", new Type[]{}, VOID);
    public static final InternalFunction SHA1_Update = new InternalFunction("SHA1_Update", new Type[]{}, VOID);
    public static final InternalFunction SSL_COMP_get_compression_methods = new InternalFunction("SSL_COMP_get_compression_methods", new Type[]{}, VOID);
    public static final InternalFunction SSL_CTX_free = new InternalFunction("SSL_CTX_free", new Type[]{}, VOID);
    public static final InternalFunction SSL_CTX_load_verify_locations = new InternalFunction("SSL_CTX_load_verify_locations", new Type[]{}, VOID);
    public static final InternalFunction SSL_CTX_new = new InternalFunction("SSL_CTX_new", new Type[]{}, VOID);
    public static final InternalFunction SSL_CTX_set_verify = new InternalFunction("SSL_CTX_set_verify", new Type[]{}, VOID);
    public static final InternalFunction SSL_CTX_use_PrivateKey_file = new InternalFunction("SSL_CTX_use_PrivateKey_file", new Type[]{}, VOID);
    public static final InternalFunction SSL_CTX_use_certificate_chain_file = new InternalFunction("SSL_CTX_use_certificate_chain_file", new Type[]{}, VOID);
    public static final InternalFunction SSL_accept = new InternalFunction("SSL_accept", new Type[]{}, VOID);
    public static final InternalFunction SSL_connect = new InternalFunction("SSL_connect", new Type[]{}, VOID);
    public static final InternalFunction SSL_ctrl = new InternalFunction("SSL_ctrl", new Type[]{}, VOID);
    public static final InternalFunction SSL_free = new InternalFunction("SSL_free", new Type[]{}, VOID);
    public static final InternalFunction SSL_get_error = new InternalFunction("SSL_get_error", new Type[]{}, VOID);
    public static final InternalFunction SSL_get_peer_certificate = new InternalFunction("SSL_get_peer_certificate", new Type[]{}, VOID);
    public static final InternalFunction SSL_get_shutdown = new InternalFunction("SSL_get_shutdown", new Type[]{}, VOID);
    public static final InternalFunction SSL_get_verify_result = new InternalFunction("SSL_get_verify_result", new Type[]{}, VOID);
    public static final InternalFunction SSL_library_init = new InternalFunction("SSL_library_init", new Type[]{}, VOID);
    public static final InternalFunction SSL_load_error_strings = new InternalFunction("SSL_load_error_strings", new Type[]{}, VOID);
    public static final InternalFunction SSL_new = new InternalFunction("SSL_new", new Type[]{}, VOID);
    public static final InternalFunction SSL_pending = new InternalFunction("SSL_pending", new Type[]{}, VOID);
    public static final InternalFunction SSL_read = new InternalFunction("SSL_read", new Type[]{}, VOID);
    public static final InternalFunction SSL_set_bio = new InternalFunction("SSL_set_bio", new Type[]{}, VOID);
    public static final InternalFunction SSL_set_fd = new InternalFunction("SSL_set_fd", new Type[]{}, VOID);
    public static final InternalFunction SSL_shutdown = new InternalFunction("SSL_shutdown", new Type[]{}, VOID);
    public static final InternalFunction SSL_write = new InternalFunction("SSL_write", new Type[]{}, VOID);
    public static final InternalFunction SSLv23_client_method = new InternalFunction("SSLv23_client_method", new Type[]{}, VOID);
    public static final InternalFunction TLSv1_client_method = new InternalFunction("TLSv1_client_method", new Type[]{}, VOID);
    public static final InternalFunction TLSv1_server_method = new InternalFunction("TLSv1_server_method", new Type[]{}, VOID);
    public static final InternalFunction X509_NAME_free = new InternalFunction("X509_NAME_free", new Type[]{}, VOID);
    public static final InternalFunction X509_NAME_get_text_by_NID = new InternalFunction("X509_NAME_get_text_by_NID", new Type[]{}, VOID);
    public static final InternalFunction X509_NAME_oneline = new InternalFunction("X509_NAME_oneline", new Type[]{}, VOID);
    public static final InternalFunction X509_NAME_print_ex = new InternalFunction("X509_NAME_print_ex", new Type[]{}, VOID);
    public static final InternalFunction X509_free = new InternalFunction("X509_free", new Type[]{}, VOID);
    public static final InternalFunction X509_get_issuer_name = new InternalFunction("X509_get_issuer_name", new Type[]{}, VOID);
    public static final InternalFunction X509_get_pubkey = new InternalFunction("X509_get_pubkey", new Type[]{}, VOID);
    public static final InternalFunction X509_get_serialNumber = new InternalFunction("X509_get_serialNumber", new Type[]{}, VOID);
    public static final InternalFunction X509_get_subject_name = new InternalFunction("X509_get_subject_name", new Type[]{}, VOID);
    public static final InternalFunction X509_new = new InternalFunction("X509_new", new Type[]{}, VOID);
    public static final InternalFunction XML_ErrorString = new InternalFunction("XML_ErrorString", new Type[]{}, VOID);
    public static final InternalFunction XML_GetBuffer = new InternalFunction("XML_GetBuffer", new Type[]{}, VOID);
    public static final InternalFunction XML_GetCurrentLineNumber = new InternalFunction("XML_GetCurrentLineNumber", new Type[]{}, VOID);
    public static final InternalFunction XML_GetErrorCode = new InternalFunction("XML_GetErrorCode", new Type[]{}, VOID);
    public static final InternalFunction XML_ParseBuffer = new InternalFunction("XML_ParseBuffer", new Type[]{}, VOID);
    public static final InternalFunction XML_ParserCreate = new InternalFunction("XML_ParserCreate", new Type[]{}, VOID);
    public static final InternalFunction XML_ParserFree = new InternalFunction("XML_ParserFree", new Type[]{}, VOID);
    public static final InternalFunction XML_SetCharacterDataHandler = new InternalFunction("XML_SetCharacterDataHandler", new Type[]{}, VOID);
    public static final InternalFunction XML_SetDoctypeDeclHandler = new InternalFunction("XML_SetDoctypeDeclHandler", new Type[]{}, VOID);
    public static final InternalFunction XML_SetElementHandler = new InternalFunction("XML_SetElementHandler", new Type[]{}, VOID);
    public static final InternalFunction XML_SetUserData = new InternalFunction("XML_SetUserData", new Type[]{}, VOID);
    public static final InternalFunction _ZN11GraphicsJNI15getNativeBitmapEP7_JNIEnvP8_jobject = new InternalFunction("_ZN11GraphicsJNI15getNativeBitmapEP7_JNIEnvP8_jobject", new Type[]{}, VOID);
    public static final InternalFunction _ZN7SkPaint12setAntiAliasEb = new InternalFunction("_ZN7SkPaint12setAntiAliasEb", new Type[]{}, VOID);
    public static final InternalFunction _ZN7SkPaint15setFilterBitmapEb = new InternalFunction("_ZN7SkPaint15setFilterBitmapEb", new Type[]{}, VOID);
    public static final InternalFunction _ZN7SkPaint9setDitherEb = new InternalFunction("_ZN7SkPaint9setDitherEb", new Type[]{}, VOID);
    public static final InternalFunction _ZN7SkPaintC1Ev = new InternalFunction("_ZN7SkPaintC1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7SkPaintD1Ev = new InternalFunction("_ZN7SkPaintD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10AudioTrack11getPositionEPj = new InternalFunction("_ZN7android10AudioTrack11getPositionEPj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10AudioTrack4stopEv = new InternalFunction("_ZN7android10AudioTrack4stopEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10AudioTrack5flushEv = new InternalFunction("_ZN7android10AudioTrack5flushEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10AudioTrack5pauseEv = new InternalFunction("_ZN7android10AudioTrack5pauseEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10AudioTrack5startEv = new InternalFunction("_ZN7android10AudioTrack5startEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10AudioTrack5writeEPKvj = new InternalFunction("_ZN7android10AudioTrack5writeEPKvj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10AudioTrack9setVolumeEff = new InternalFunction("_ZN7android10AudioTrack9setVolumeEff", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10AudioTrackC1EijiiijPFviPvS1_ES1_i = new InternalFunction("_ZN7android10AudioTrackC1EijiiijPFviPvS1_ES1_i", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10AudioTrackC1EijiiijPFviPvS1_ES1_ii = new InternalFunction("_ZN7android10AudioTrackC1EijiiijPFviPvS1_ES1_ii", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10AudioTrackD1Ev = new InternalFunction("_ZN7android10AudioTrackD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10IInterface8asBinderEv = new InternalFunction("_ZN7android10IInterface8asBinderEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10IInterfaceD0Ev = new InternalFunction("_ZN7android10IInterfaceD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10IInterfaceD1Ev = new InternalFunction("_ZN7android10IInterfaceD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl13finish_vectorEv = new InternalFunction("_ZN7android10VectorImpl13finish_vectorEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl13removeItemsAtEjj = new InternalFunction("_ZN7android10VectorImpl13removeItemsAtEjj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl16editItemLocationEj = new InternalFunction("_ZN7android10VectorImpl16editItemLocationEj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl19reservedVectorImpl1Ev = new InternalFunction("_ZN7android10VectorImpl19reservedVectorImpl1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl19reservedVectorImpl2Ev = new InternalFunction("_ZN7android10VectorImpl19reservedVectorImpl2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl19reservedVectorImpl3Ev = new InternalFunction("_ZN7android10VectorImpl19reservedVectorImpl3Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl19reservedVectorImpl4Ev = new InternalFunction("_ZN7android10VectorImpl19reservedVectorImpl4Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl19reservedVectorImpl5Ev = new InternalFunction("_ZN7android10VectorImpl19reservedVectorImpl5Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl19reservedVectorImpl6Ev = new InternalFunction("_ZN7android10VectorImpl19reservedVectorImpl6Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl19reservedVectorImpl7Ev = new InternalFunction("_ZN7android10VectorImpl19reservedVectorImpl7Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl19reservedVectorImpl8Ev = new InternalFunction("_ZN7android10VectorImpl19reservedVectorImpl8Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl3addEPKv = new InternalFunction("_ZN7android10VectorImpl3addEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl4pushEPKv = new InternalFunction("_ZN7android10VectorImpl4pushEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl4pushEv = new InternalFunction("_ZN7android10VectorImpl4pushEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl4sortEPFiPKvS2_E = new InternalFunction("_ZN7android10VectorImpl4sortEPFiPKvS2_E", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl5clearEv = new InternalFunction("_ZN7android10VectorImpl5clearEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImpl8insertAtEPKvjj = new InternalFunction("_ZN7android10VectorImpl8insertAtEPKvjj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImplC2ERKS0_ = new InternalFunction("_ZN7android10VectorImplC2ERKS0_", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImplC2Ejj = new InternalFunction("_ZN7android10VectorImplC2Ejj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android10VectorImplD2Ev = new InternalFunction("_ZN7android10VectorImplD2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioPlayer19getMediaTimeMappingEPxS1_ = new InternalFunction("_ZN7android11AudioPlayer19getMediaTimeMappingEPxS1_", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioPlayer5pauseEb = new InternalFunction("_ZN7android11AudioPlayer5pauseEb", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioPlayer5pauseEv = new InternalFunction("_ZN7android11AudioPlayer5pauseEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioPlayer5startEb = new InternalFunction("_ZN7android11AudioPlayer5startEb", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioPlayer6resumeEv = new InternalFunction("_ZN7android11AudioPlayer6resumeEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioPlayer9setSourceERKNS_2spINS_11MediaSourceEEE = new InternalFunction("_ZN7android11AudioPlayer9setSourceERKNS_2spINS_11MediaSourceEEE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioPlayerC1ERKNS_2spINS_15MediaPlayerBase9AudioSinkEEE = new InternalFunction("_ZN7android11AudioPlayerC1ERKNS_2spINS_15MediaPlayerBase9AudioSinkEEE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioPlayerC1ERKNS_2spINS_15MediaPlayerBase9AudioSinkEEEPNS_13AwesomePlayerE = new InternalFunction("_ZN7android11AudioPlayerC1ERKNS_2spINS_15MediaPlayerBase9AudioSinkEEEPNS_13AwesomePlayerE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioSystem17newAudioSessionIdEv = new InternalFunction("_ZN7android11AudioSystem17newAudioSessionIdEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioSystem19getOutputFrameCountEPii = new InternalFunction("_ZN7android11AudioSystem19getOutputFrameCountEPii", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioSystem21getOutputSamplingRateEPii = new InternalFunction("_ZN7android11AudioSystem21getOutputSamplingRateEPii", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaBuffer11setObserverEPNS_19MediaBufferObserverE = new InternalFunction("_ZN7android11MediaBuffer11setObserverEPNS_19MediaBufferObserverE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaBuffer5cloneEv = new InternalFunction("_ZN7android11MediaBuffer5cloneEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaBuffer7add_refEv = new InternalFunction("_ZN7android11MediaBuffer7add_refEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaBuffer7releaseEv = new InternalFunction("_ZN7android11MediaBuffer7releaseEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaBuffer9meta_dataEv = new InternalFunction("_ZN7android11MediaBuffer9meta_dataEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaBuffer9set_rangeEjj = new InternalFunction("_ZN7android11MediaBuffer9set_rangeEjj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaBufferC1EPvj = new InternalFunction("_ZN7android11MediaBufferC1EPvj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaBufferC1Ej = new InternalFunction("_ZN7android11MediaBufferC1Ej", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaSource11ReadOptions11clearSeekToEv = new InternalFunction("_ZN7android11MediaSource11ReadOptions11clearSeekToEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaSource11ReadOptions9setSeekToEx = new InternalFunction("_ZN7android11MediaSource11ReadOptions9setSeekToEx", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaSource11ReadOptions9setSeekToExNS1_8SeekModeE = new InternalFunction("_ZN7android11MediaSource11ReadOptions9setSeekToExNS1_8SeekModeE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaSource11ReadOptionsC1Ev = new InternalFunction("_ZN7android11MediaSource11ReadOptionsC1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaSourceC2Ev = new InternalFunction("_ZN7android11MediaSourceC2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaSourceD0Ev = new InternalFunction("_ZN7android11MediaSourceD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaSourceD1Ev = new InternalFunction("_ZN7android11MediaSourceD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11MediaSourceD2Ev = new InternalFunction("_ZN7android11MediaSourceD2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11QueryCodecsERKNS_2spINS_4IOMXEEEPKcbPNS_6VectorINS_17CodecCapabilitiesEEE = new InternalFunction("_ZN7android11QueryCodecsERKNS_2spINS_4IOMXEEEPKcbPNS_6VectorINS_17CodecCapabilitiesEEE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android12IOMXObserverC2Ev = new InternalFunction("_ZN7android12IOMXObserverC2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android12IOMXObserverD0Ev = new InternalFunction("_ZN7android12IOMXObserverD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android12IOMXObserverD1Ev = new InternalFunction("_ZN7android12IOMXObserverD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android12IOMXObserverD2Ev = new InternalFunction("_ZN7android12IOMXObserverD2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android12MemoryDealerC1EjPKc = new InternalFunction("_ZN7android12MemoryDealerC1EjPKc", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android12ProcessState15startThreadPoolEv = new InternalFunction("_ZN7android12ProcessState15startThreadPoolEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android12ProcessState4selfEv = new InternalFunction("_ZN7android12ProcessState4selfEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android12SharedBuffer5allocEj = new InternalFunction("_ZN7android12SharedBuffer5allocEj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android12bitsPerPixelEi = new InternalFunction("_ZN7android12bitsPerPixelEi", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android13BnOMXObserver10onTransactEjRKNS_6ParcelEPS1_j = new InternalFunction("_ZN7android13BnOMXObserver10onTransactEjRKNS_6ParcelEPS1_j", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android13GraphicBuffer4lockEjPPv = new InternalFunction("_ZN7android13GraphicBuffer4lockEjPPv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android13GraphicBuffer6unlockEv = new InternalFunction("_ZN7android13GraphicBuffer6unlockEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android13GraphicBufferC1EP19ANativeWindowBufferb = new InternalFunction("_ZN7android13GraphicBufferC1EP19ANativeWindowBufferb", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android13GraphicBufferC1Ejjij = new InternalFunction("_ZN7android13GraphicBufferC1Ejjij", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android13MediaProfiles11getInstanceEv = new InternalFunction("_ZN7android13MediaProfiles11getInstanceEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android14AndroidRuntime21registerNativeMethodsEP7_JNIEnvPKcPK15JNINativeMethodi = new InternalFunction("_ZN7android14AndroidRuntime21registerNativeMethodsEP7_JNIEnvPKcPK15JNINativeMethodi", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android14AndroidRuntime9getJNIEnvEv = new InternalFunction("_ZN7android14AndroidRuntime9getJNIEnvEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android14IPCThreadState13flushCommandsEv = new InternalFunction("_ZN7android14IPCThreadState13flushCommandsEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android14IPCThreadState4selfEv = new InternalFunction("_ZN7android14IPCThreadState4selfEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android14SurfaceTexture14updateTexImageEv = new InternalFunction("_ZN7android14SurfaceTexture14updateTexImageEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android14SurfaceTexture18getTransformMatrixEPf = new InternalFunction("_ZN7android14SurfaceTexture18getTransformMatrixEPf", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android14SurfaceTexture25setFrameAvailableListenerERKNS_2spINS0_22FrameAvailableListenerEEE = new InternalFunction("_ZN7android14SurfaceTexture25setFrameAvailableListenerERKNS_2spINS0_22FrameAvailableListenerEEE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android14SurfaceTextureC1Ej = new InternalFunction("_ZN7android14SurfaceTextureC1Ej", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16MediaBufferGroup10add_bufferEPNS_11MediaBufferE = new InternalFunction("_ZN7android16MediaBufferGroup10add_bufferEPNS_11MediaBufferE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16MediaBufferGroup14acquire_bufferEPPNS_11MediaBufferE = new InternalFunction("_ZN7android16MediaBufferGroup14acquire_bufferEPPNS_11MediaBufferE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16MediaBufferGroupC1Ev = new InternalFunction("_ZN7android16MediaBufferGroupC1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16SortedVectorImpl25reservedSortedVectorImpl1Ev = new InternalFunction("_ZN7android16SortedVectorImpl25reservedSortedVectorImpl1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16SortedVectorImpl25reservedSortedVectorImpl2Ev = new InternalFunction("_ZN7android16SortedVectorImpl25reservedSortedVectorImpl2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16SortedVectorImpl25reservedSortedVectorImpl3Ev = new InternalFunction("_ZN7android16SortedVectorImpl25reservedSortedVectorImpl3Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16SortedVectorImpl25reservedSortedVectorImpl4Ev = new InternalFunction("_ZN7android16SortedVectorImpl25reservedSortedVectorImpl4Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16SortedVectorImpl25reservedSortedVectorImpl5Ev = new InternalFunction("_ZN7android16SortedVectorImpl25reservedSortedVectorImpl5Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16SortedVectorImpl25reservedSortedVectorImpl6Ev = new InternalFunction("_ZN7android16SortedVectorImpl25reservedSortedVectorImpl6Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16SortedVectorImpl25reservedSortedVectorImpl7Ev = new InternalFunction("_ZN7android16SortedVectorImpl25reservedSortedVectorImpl7Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16SortedVectorImpl25reservedSortedVectorImpl8Ev = new InternalFunction("_ZN7android16SortedVectorImpl25reservedSortedVectorImpl8Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16SortedVectorImpl3addEPKv = new InternalFunction("_ZN7android16SortedVectorImpl3addEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16SortedVectorImplC2Ejj = new InternalFunction("_ZN7android16SortedVectorImplC2Ejj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android16SortedVectorImplD2Ev = new InternalFunction("_ZN7android16SortedVectorImplD2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19IMediaDeathNotifier21getMediaPlayerServiceEv = new InternalFunction("_ZN7android19IMediaDeathNotifier21getMediaPlayerServiceEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19IMediaPlayerService11asInterfaceERKNS_2spINS_7IBinderEEE = new InternalFunction("_ZN7android19IMediaPlayerService11asInterfaceERKNS_2spINS_7IBinderEEE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19GraphicBufferMapper14registerBufferEPK13native_handle = new InternalFunction("_ZN7android19GraphicBufferMapper14registerBufferEPK13native_handle", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19GraphicBufferMapper14registerBufferEPK15native_handle_t = new InternalFunction("_ZN7android19GraphicBufferMapper14registerBufferEPK15native_handle_t", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19GraphicBufferMapper16unregisterBufferEPK13native_handle = new InternalFunction("_ZN7android19GraphicBufferMapper16unregisterBufferEPK13native_handle", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19GraphicBufferMapper16unregisterBufferEPK15native_handle_t = new InternalFunction("_ZN7android19GraphicBufferMapper16unregisterBufferEPK15native_handle_t", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19GraphicBufferMapper4lockEPK15native_handle_tiRKNS_4RectEPPv = new InternalFunction("_ZN7android19GraphicBufferMapper4lockEPK15native_handle_tiRKNS_4RectEPPv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19GraphicBufferMapper6unlockEPK15native_handle_t = new InternalFunction("_ZN7android19GraphicBufferMapper6unlockEPK15native_handle_t", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19GraphicBufferMapperC1Ev = new InternalFunction("_ZN7android19GraphicBufferMapperC1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19GraphicBufferMapperC2Ev = new InternalFunction("_ZN7android19GraphicBufferMapperC2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19GraphicBufferMapper6unlockEPK13native_handle = new InternalFunction("_ZN7android19GraphicBufferMapper6unlockEPK13native_handle", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19GraphicBufferMapper4lockEPK13native_handleiRKNS_4RectEPPv = new InternalFunction("_ZN7android19GraphicBufferMapper4lockEPK13native_handleiRKNS_4RectEPPv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android19parcelForJavaObjectEP7_JNIEnvP8_jobject = new InternalFunction("_ZN7android19parcelForJavaObjectEP7_JNIEnvP8_jobject", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android20SurfaceTextureClientC1ERKNS_2spINS_15ISurfaceTextureEEE = new InternalFunction("_ZN7android20SurfaceTextureClientC1ERKNS_2spINS_15ISurfaceTextureEEE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android20ibinderForJavaObjectEP7_JNIEnvP8_jobject = new InternalFunction("_ZN7android20ibinderForJavaObjectEP7_JNIEnvP8_jobject", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android20javaObjectForIBinderEP7_JNIEnvRKNS_2spINS_7IBinderEEE = new InternalFunction("_ZN7android20javaObjectForIBinderEP7_JNIEnvRKNS_2spINS_7IBinderEEE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android21defaultServiceManagerEv = new InternalFunction("_ZN7android21defaultServiceManagerEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android38android_SurfaceTexture_getNativeWindowEP7_JNIEnvP8_jobject = new InternalFunction("_ZN7android38android_SurfaceTexture_getNativeWindowEP7_JNIEnvP8_jobject", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android4IOMX14createRendererERKNS_2spINS_7SurfaceEEEPKc20OMX_COLOR_FORMATTYPEjjjji = new InternalFunction("_ZN7android4IOMX14createRendererERKNS_2spINS_7SurfaceEEEPKc20OMX_COLOR_FORMATTYPEjjjji", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android4IOMX29createRendererFromJavaSurfaceEP7_JNIEnvP8_jobjectPKc20OMX_COLOR_FORMATTYPEjjjji = new InternalFunction("_ZN7android4IOMX29createRendererFromJavaSurfaceEP7_JNIEnvP8_jobjectPKc20OMX_COLOR_FORMATTYPEjjjji", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6Parcel10writeFloatEf = new InternalFunction("_ZN7android6Parcel10writeFloatEf", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6Parcel10writeInt32Ei = new InternalFunction("_ZN7android6Parcel10writeInt32Ei", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6Parcel10writeInt64Ex = new InternalFunction("_ZN7android6Parcel10writeInt64Ex", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6Parcel11writeDoubleEd = new InternalFunction("_ZN7android6Parcel11writeDoubleEd", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6Parcel12writeCStringEPKc = new InternalFunction("_ZN7android6Parcel12writeCStringEPKc", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6Parcel19writeInterfaceTokenERKNS_8String16E = new InternalFunction("_ZN7android6Parcel19writeInterfaceTokenERKNS_8String16E", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6Thread10readyToRunEv = new InternalFunction("_ZN7android6Thread10readyToRunEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6Thread11requestExitEv = new InternalFunction("_ZN7android6Thread11requestExitEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6Thread18requestExitAndWaitEv = new InternalFunction("_ZN7android6Thread18requestExitAndWaitEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6Thread3runEPKcij = new InternalFunction("_ZN7android6Thread3runEPKcij", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6ThreadC2Eb = new InternalFunction("_ZN7android6ThreadC2Eb", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6ThreadD0Ev = new InternalFunction("_ZN7android6ThreadD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6ThreadD1Ev = new InternalFunction("_ZN7android6ThreadD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android6ThreadD2Ev = new InternalFunction("_ZN7android6ThreadD2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinder10onTransactEjRKNS_6ParcelEPS1_j = new InternalFunction("_ZN7android7BBinder10onTransactEjRKNS_6ParcelEPS1_j", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinder10pingBinderEv = new InternalFunction("_ZN7android7BBinder10pingBinderEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinder11linkToDeathERKNS_2spINS_7IBinder14DeathRecipientEEEPvj = new InternalFunction("_ZN7android7BBinder11linkToDeathERKNS_2spINS_7IBinder14DeathRecipientEEEPvj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinder11localBinderEv = new InternalFunction("_ZN7android7BBinder11localBinderEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinder12attachObjectEPKvPvS3_PFvS2_S3_S3_E = new InternalFunction("_ZN7android7BBinder12attachObjectEPKvPvS3_PFvS2_S3_S3_E", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinder12detachObjectEPKv = new InternalFunction("_ZN7android7BBinder12detachObjectEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinder13unlinkToDeathERKNS_2wpINS_7IBinder14DeathRecipientEEEPvjPS4_ = new InternalFunction("_ZN7android7BBinder13unlinkToDeathERKNS_2wpINS_7IBinder14DeathRecipientEEEPvjPS4_", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinder4dumpEiRKNS_6VectorINS_8String16EEE = new InternalFunction("_ZN7android7BBinder4dumpEiRKNS_6VectorINS_8String16EEE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinder8transactEjRKNS_6ParcelEPS1_j = new InternalFunction("_ZN7android7BBinder8transactEjRKNS_6ParcelEPS1_j", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinderC2Ev = new InternalFunction("_ZN7android7BBinderC2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinderD0Ev = new InternalFunction("_ZN7android7BBinderD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinderD1Ev = new InternalFunction("_ZN7android7BBinderD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7BBinderD2Ev = new InternalFunction("_ZN7android7BBinderD2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7IBinder11localBinderEv = new InternalFunction("_ZN7android7IBinder11localBinderEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7IBinder12remoteBinderEv = new InternalFunction("_ZN7android7IBinder12remoteBinderEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7IBinder19queryLocalInterfaceERKNS_8String16E = new InternalFunction("_ZN7android7IBinder19queryLocalInterfaceERKNS_8String16E", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7IBinderD0Ev = new InternalFunction("_ZN7android7IBinderD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7IBinderD1Ev = new InternalFunction("_ZN7android7IBinderD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7IMemory11asInterfaceERKNS_2spINS_7IBinderEEE = new InternalFunction("_ZN7android7IMemory11asInterfaceERKNS_2spINS_7IBinderEEE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7RefBase10onFirstRefEv = new InternalFunction("_ZN7android7RefBase10onFirstRefEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7RefBase12weakref_type16attemptIncStrongEPKv = new InternalFunction("_ZN7android7RefBase12weakref_type16attemptIncStrongEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7RefBase12weakref_type7decWeakEPKv = new InternalFunction("_ZN7android7RefBase12weakref_type7decWeakEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7RefBase12weakref_type7incWeakEPKv = new InternalFunction("_ZN7android7RefBase12weakref_type7incWeakEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7RefBase13onLastWeakRefEPKv = new InternalFunction("_ZN7android7RefBase13onLastWeakRefEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7RefBase15onLastStrongRefEPKv = new InternalFunction("_ZN7android7RefBase15onLastStrongRefEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7RefBase20onIncStrongAttemptedEjPKv = new InternalFunction("_ZN7android7RefBase20onIncStrongAttemptedEjPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7RefBaseC2Ev = new InternalFunction("_ZN7android7RefBaseC2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7RefBaseD2Ev = new InternalFunction("_ZN7android7RefBaseD2Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7String85setToEPKc = new InternalFunction("_ZN7android7String85setToEPKc", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7String85setToERKS0_ = new InternalFunction("_ZN7android7String85setToERKS0_", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7String86appendEPKc = new InternalFunction("_ZN7android7String86appendEPKc", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7String8C1EPKc = new InternalFunction("_ZN7android7String8C1EPKc", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7String8C1ERKS0_ = new InternalFunction("_ZN7android7String8C1ERKS0_", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7String8C1Ev = new InternalFunction("_ZN7android7String8C1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7String8D1Ev = new InternalFunction("_ZN7android7String8D1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7Surface13unlockAndPostEv = new InternalFunction("_ZN7android7Surface13unlockAndPostEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7Surface18setBuffersGeometryEiii = new InternalFunction("_ZN7android7Surface18setBuffersGeometryEiii", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7Surface4lockEPNS0_11SurfaceInfoEPNS_6RegionE = new InternalFunction("_ZN7android7Surface4lockEPNS0_11SurfaceInfoEPNS_6RegionE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7Surface4lockEPNS0_11SurfaceInfoEb = new InternalFunction("_ZN7android7Surface4lockEPNS0_11SurfaceInfoEb", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7Surface7isValidEv = new InternalFunction("_ZN7android7Surface7isValidEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android7Surface8setUsageEj = new InternalFunction("_ZN7android7Surface8setUsageEj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaData10setCStringEjPKc = new InternalFunction("_ZN7android8MetaData10setCStringEjPKc", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaData11findCStringEjPPKc = new InternalFunction("_ZN7android8MetaData11findCStringEjPPKc", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaData11findPointerEjPPv = new InternalFunction("_ZN7android8MetaData11findPointerEjPPv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaData5clearEv = new InternalFunction("_ZN7android8MetaData5clearEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaData6removeEj = new InternalFunction("_ZN7android8MetaData6removeEj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaData7setDataEjjPKvj = new InternalFunction("_ZN7android8MetaData7setDataEjjPKvj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaData8findRectEjPiS1_S1_S1_ = new InternalFunction("_ZN7android8MetaData8findRectEjPiS1_S1_S1_", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaData8setInt32Eji = new InternalFunction("_ZN7android8MetaData8setInt32Eji", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaData8setInt64Ejx = new InternalFunction("_ZN7android8MetaData8setInt64Ejx", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaData9findInt32EjPi = new InternalFunction("_ZN7android8MetaData9findInt32EjPi", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaData9findInt64EjPx = new InternalFunction("_ZN7android8MetaData9findInt64EjPx", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8MetaDataC1Ev = new InternalFunction("_ZN7android8MetaDataC1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8OMXCodec16initOutputFormatERKNS_2spINS_8MetaDataEEE = new InternalFunction("_ZN7android8OMXCodec16initOutputFormatERKNS_2spINS_8MetaDataEEE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8OMXCodec22setVideoPortFormatTypeEm20OMX_VIDEO_CODINGTYPE20OMX_COLOR_FORMATTYPE = new InternalFunction("_ZN7android8OMXCodec22setVideoPortFormatTypeEm20OMX_VIDEO_CODINGTYPE20OMX_COLOR_FORMATTYPE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8OMXCodec6CreateERKNS_2spINS_4IOMXEEERKNS1_INS_8MetaDataEEEbRKNS1_INS_11MediaSourceEEEPKcj = new InternalFunction("_ZN7android8OMXCodec6CreateERKNS_2spINS_4IOMXEEERKNS1_INS_8MetaDataEEEbRKNS1_INS_11MediaSourceEEEPKcj", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8OMXCodec6CreateERKNS_2spINS_4IOMXEEERKNS1_INS_8MetaDataEEEbRKNS1_INS_11MediaSourceEEEPKcjRKNS1_I13ANativeWindowEE = new InternalFunction("_ZN7android8OMXCodec6CreateERKNS_2spINS_4IOMXEEERKNS1_INS_8MetaDataEEEbRKNS1_INS_11MediaSourceEEEPKcjRKNS1_I13ANativeWindowEE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8OMXCodec8setStateENS0_5StateE = new InternalFunction("_ZN7android8OMXCodec8setStateENS0_5StateE", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8String16C1EPKc = new InternalFunction("_ZN7android8String16C1EPKc", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android8String16D1Ev = new InternalFunction("_ZN7android8String16D1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android9OMXClient10disconnectEv = new InternalFunction("_ZN7android9OMXClient10disconnectEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android9OMXClient7connectEv = new InternalFunction("_ZN7android9OMXClient7connectEv", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android9OMXClientC1Ev = new InternalFunction("_ZN7android9OMXClientC1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioSystem16getOutputLatencyEPj19audio_stream_type_t = new InternalFunction("_ZN7android11AudioSystem16getOutputLatencyEPj19audio_stream_type_t", new Type[]{}, VOID);
    public static final InternalFunction _ZN7android11AudioSystem16getOutputLatencyEPji = new InternalFunction("_ZN7android11AudioSystem16getOutputLatencyEPji", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkBitmap9setConfigENS_6ConfigEiii = new InternalFunction("_ZN8SkBitmap9setConfigENS_6ConfigEiii", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkBitmap9setPixelsEPvP12SkColorTable = new InternalFunction("_ZN8SkBitmap9setPixelsEPvP12SkColorTable", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkBitmapC1Ev = new InternalFunction("_ZN8SkBitmapC1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkBitmapD1Ev = new InternalFunction("_ZN8SkBitmapD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkCanvas14drawBitmapRectERK8SkBitmapPK7SkIRectRK6SkRectPK7SkPaint = new InternalFunction("_ZN8SkCanvas14drawBitmapRectERK8SkBitmapPK7SkIRectRK6SkRectPK7SkPaint", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkCanvas15setBitmapDeviceERK8SkBitmap = new InternalFunction("_ZN8SkCanvas15setBitmapDeviceERK8SkBitmap", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkCanvas15setBitmapDeviceERK8SkBitmapb = new InternalFunction("_ZN8SkCanvas15setBitmapDeviceERK8SkBitmapb", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkCanvas9drawColorEjN10SkXfermode4ModeE = new InternalFunction("_ZN8SkCanvas9drawColorEjN10SkXfermode4ModeE", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkCanvas9drawColorEjN12SkPorterDuff4ModeE = new InternalFunction("_ZN8SkCanvas9drawColorEjN12SkPorterDuff4ModeE", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkCanvasC1EP15SkDeviceFactory = new InternalFunction("_ZN8SkCanvasC1EP15SkDeviceFactory", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkCanvasC1EP8SkDevice = new InternalFunction("_ZN8SkCanvasC1EP8SkDevice", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkCanvasC1Ev = new InternalFunction("_ZN8SkCanvasC1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZN8SkCanvasD1Ev = new InternalFunction("_ZN8SkCanvasD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android10AudioTrack10frameCountEv = new InternalFunction("_ZNK7android10AudioTrack10frameCountEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android10AudioTrack12channelCountEv = new InternalFunction("_ZNK7android10AudioTrack12channelCountEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android10AudioTrack4dumpEiRKNS_6VectorINS_8String16EEE = new InternalFunction("_ZNK7android10AudioTrack4dumpEiRKNS_6VectorINS_8String16EEE", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android10AudioTrack7latencyEv = new InternalFunction("_ZNK7android10AudioTrack7latencyEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android10AudioTrack9frameSizeEv = new InternalFunction("_ZNK7android10AudioTrack9frameSizeEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android10AudioTrack9initCheckEv = new InternalFunction("_ZNK7android10AudioTrack9initCheckEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android11MediaBuffer12range_lengthEv = new InternalFunction("_ZNK7android11MediaBuffer12range_lengthEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android11MediaBuffer12range_offsetEv = new InternalFunction("_ZNK7android11MediaBuffer12range_offsetEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android11MediaBuffer13graphicBufferEv = new InternalFunction("_ZNK7android11MediaBuffer13graphicBufferEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android11MediaBuffer4dataEv = new InternalFunction("_ZNK7android11MediaBuffer4dataEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android11MediaBuffer4sizeEv = new InternalFunction("_ZNK7android11MediaBuffer4sizeEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android11MediaBuffer8refcountEv = new InternalFunction("_ZNK7android11MediaBuffer8refcountEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android11MediaSource11ReadOptions9getSeekToEPxPNS1_8SeekModeE = new InternalFunction("_ZNK7android11MediaSource11ReadOptions9getSeekToEPxPNS1_8SeekModeE", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android12IOMXObserver22getInterfaceDescriptorEv = new InternalFunction("_ZNK7android12IOMXObserver22getInterfaceDescriptorEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android12SharedBuffer10editResizeEj = new InternalFunction("_ZNK7android12SharedBuffer10editResizeEj", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android12SharedBuffer4editEv = new InternalFunction("_ZNK7android12SharedBuffer4editEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android12SharedBuffer7acquireEv = new InternalFunction("_ZNK7android12SharedBuffer7acquireEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android12SharedBuffer7releaseEj = new InternalFunction("_ZNK7android12SharedBuffer7releaseEj", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android13GraphicBuffer15getNativeBufferEv = new InternalFunction("_ZNK7android13GraphicBuffer15getNativeBufferEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android13GraphicBuffer9initCheckEv = new InternalFunction("_ZNK7android13GraphicBuffer9initCheckEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android13MediaProfiles28getVideoEditorCapParamByNameEPKc = new InternalFunction("_ZNK7android13MediaProfiles28getVideoEditorCapParamByNameEPKc", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android13MediaProfiles31getVideoEditorExportParamByNameEPKci = new InternalFunction("_ZNK7android13MediaProfiles31getVideoEditorExportParamByNameEPKci", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android16SortedVectorImpl7indexOfEPKv = new InternalFunction("_ZNK7android16SortedVectorImpl7indexOfEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android6Parcel10readDoubleEv = new InternalFunction("_ZNK7android6Parcel10readDoubleEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android6Parcel11readCStringEv = new InternalFunction("_ZNK7android6Parcel11readCStringEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android6Parcel12dataPositionEv = new InternalFunction("_ZNK7android6Parcel12dataPositionEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android6Parcel14checkInterfaceEPNS_7IBinderE = new InternalFunction("_ZNK7android6Parcel14checkInterfaceEPNS_7IBinderE", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android6Parcel19readString16InplaceEPj = new InternalFunction("_ZNK7android6Parcel19readString16InplaceEPj", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android6Parcel4readEPvj = new InternalFunction("_ZNK7android6Parcel4readEPvj", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android6Parcel9readFloatEv = new InternalFunction("_ZNK7android6Parcel9readFloatEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android6Parcel9readInt32EPi = new InternalFunction("_ZNK7android6Parcel9readInt32EPi", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android6Parcel9readInt32Ev = new InternalFunction("_ZNK7android6Parcel9readInt32Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android6Parcel9readInt64Ev = new InternalFunction("_ZNK7android6Parcel9readInt64Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android7BBinder10findObjectEPKv = new InternalFunction("_ZNK7android7BBinder10findObjectEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android7BBinder13isBinderAliveEv = new InternalFunction("_ZNK7android7BBinder13isBinderAliveEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android7BBinder22getInterfaceDescriptorEv = new InternalFunction("_ZNK7android7BBinder22getInterfaceDescriptorEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android7IBinder13checkSubclassEPKv = new InternalFunction("_ZNK7android7IBinder13checkSubclassEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android7IMemory4sizeEv = new InternalFunction("_ZNK7android7IMemory4sizeEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android7IMemory6offsetEv = new InternalFunction("_ZNK7android7IMemory6offsetEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android7IMemory7pointerEv = new InternalFunction("_ZNK7android7IMemory7pointerEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android7RefBase10createWeakEPKv = new InternalFunction("_ZNK7android7RefBase10createWeakEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android7RefBase9decStrongEPKv = new InternalFunction("_ZNK7android7RefBase9decStrongEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android7RefBase9incStrongEPKv = new InternalFunction("_ZNK7android7RefBase9incStrongEPKv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android7Surface11getISurfaceEv = new InternalFunction("_ZNK7android7Surface11getISurfaceEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android8MetaData8findDataEjPjPPKvS1_ = new InternalFunction("_ZNK7android8MetaData8findDataEjPjPPKvS1_", new Type[]{}, VOID);
    public static final InternalFunction _ZNK7android8MetaData9dumpToLogEv = new InternalFunction("_ZNK7android8MetaData9dumpToLogEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK8SkBitmap10lockPixelsEv = new InternalFunction("_ZNK8SkBitmap10lockPixelsEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNK8SkBitmap12unlockPixelsEv = new InternalFunction("_ZNK8SkBitmap12unlockPixelsEv", new Type[]{}, VOID);
    public static final InternalFunction _ZNSt12__node_alloc11_M_allocateERj = new InternalFunction("_ZNSt12__node_alloc11_M_allocateERj", new Type[]{}, VOID);
    public static final InternalFunction _ZNSt12__node_alloc13_M_deallocateEPvj = new InternalFunction("_ZNSt12__node_alloc13_M_deallocateEPvj", new Type[]{}, VOID);
    public static final InternalFunction _ZThn4_N7android13BnOMXObserver10onTransactEjRKNS_6ParcelEPS1_j = new InternalFunction("_ZThn4_N7android13BnOMXObserver10onTransactEjRKNS_6ParcelEPS1_j", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android10IInterfaceD0Ev = new InternalFunction("_ZTv0_n12_N7android10IInterfaceD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android10IInterfaceD1Ev = new InternalFunction("_ZTv0_n12_N7android10IInterfaceD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android11MediaSourceD0Ev = new InternalFunction("_ZTv0_n12_N7android11MediaSourceD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android11MediaSourceD1Ev = new InternalFunction("_ZTv0_n12_N7android11MediaSourceD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android12IOMXObserverD0Ev = new InternalFunction("_ZTv0_n12_N7android12IOMXObserverD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android12IOMXObserverD1Ev = new InternalFunction("_ZTv0_n12_N7android12IOMXObserverD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android6ThreadD0Ev = new InternalFunction("_ZTv0_n12_N7android6ThreadD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android6ThreadD1Ev = new InternalFunction("_ZTv0_n12_N7android6ThreadD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android7BBinderD0Ev = new InternalFunction("_ZTv0_n12_N7android7BBinderD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android7BBinderD1Ev = new InternalFunction("_ZTv0_n12_N7android7BBinderD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android7IBinderD0Ev = new InternalFunction("_ZTv0_n12_N7android7IBinderD0Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZTv0_n12_N7android7IBinderD1Ev = new InternalFunction("_ZTv0_n12_N7android7IBinderD1Ev", new Type[]{}, VOID);
    public static final InternalFunction _ZdlPvRKSt9nothrow_t = new InternalFunction("_ZdlPvRKSt9nothrow_t", new Type[]{}, VOID);
    public static final InternalFunction _ZnwjRKSt9nothrow_t = new InternalFunction("_ZnwjRKSt9nothrow_t", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_atexit = new InternalFunction("__aeabi_atexit", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_d2f = new InternalFunction("__aeabi_d2f", new Type[]{DOUBLE}, FLOAT);
    public static final InternalFunction __aeabi_d2iz = new InternalFunction("__aeabi_d2iz", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_d2lz = new InternalFunction("__aeabi_d2lz", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_d2ulz = new InternalFunction("__aeabi_d2ulz", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_dadd = new InternalFunction("__aeabi_dadd", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_dcmpeq = new InternalFunction("__aeabi_dcmpeq", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_dcmpge = new InternalFunction("__aeabi_dcmpge", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_dcmpgt = new InternalFunction("__aeabi_dcmpgt", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_dcmple = new InternalFunction("__aeabi_dcmple", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_dcmplt = new InternalFunction("__aeabi_dcmplt", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_dcmpun = new InternalFunction("__aeabi_dcmpun", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_ddiv = new InternalFunction("__aeabi_ddiv", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_dmul = new InternalFunction("__aeabi_dmul", new Type[]{DOUBLE, DOUBLE}, DOUBLE);
    public static final InternalFunction __aeabi_dsub = new InternalFunction("__aeabi_dsub", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_f2d = new InternalFunction("__aeabi_f2d", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_f2iz = new InternalFunction("__aeabi_f2iz", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_f2uiz = new InternalFunction("__aeabi_f2uiz", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_fadd = new InternalFunction("__aeabi_fadd", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_fcmpeq = new InternalFunction("__aeabi_fcmpeq", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_fcmpge = new InternalFunction("__aeabi_fcmpge", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_fcmpgt = new InternalFunction("__aeabi_fcmpgt", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_fcmple = new InternalFunction("__aeabi_fcmple", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_fcmplt = new InternalFunction("__aeabi_fcmplt", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_fcmpun = new InternalFunction("__aeabi_fcmpun", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_fdiv = new InternalFunction("__aeabi_fdiv", new Type[]{FLOAT, FLOAT}, FLOAT);
    public static final InternalFunction __aeabi_fmul = new InternalFunction("__aeabi_fmul", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_fsub = new InternalFunction("__aeabi_fsub", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_i2d = new InternalFunction("__aeabi_i2d", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_i2f = new InternalFunction("__aeabi_i2f", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_idiv = new InternalFunction("__aeabi_idiv", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_idivmod = new InternalFunction("__aeabi_idivmod", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_l2d = new InternalFunction("__aeabi_l2d", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_l2f = new InternalFunction("__aeabi_l2f", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_ldivmod = new InternalFunction("__aeabi_ldivmod", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_lmul = new InternalFunction("__aeabi_lmul", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_memclr = new InternalFunction("__aeabi_memclr", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_memclr4 = new InternalFunction("__aeabi_memclr4", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_memcpy4 = new InternalFunction("__aeabi_memcpy4", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_memmove = new InternalFunction("__aeabi_memmove", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_memmove4 = new InternalFunction("__aeabi_memmove4", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_ui2d = new InternalFunction("__aeabi_ui2d", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_ui2f = new InternalFunction("__aeabi_ui2f", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_uidiv = new InternalFunction("__aeabi_uidiv", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_uidivmod = new InternalFunction("__aeabi_uidivmod", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_ul2f = new InternalFunction("__aeabi_ul2f", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_uldivmod = new InternalFunction("__aeabi_uldivmod", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_unwind_cpp_pr0 = new InternalFunction("__aeabi_unwind_cpp_pr0", new Type[]{}, VOID);
    public static final InternalFunction __aeabi_unwind_cpp_pr1 = new InternalFunction("__aeabi_unwind_cpp_pr1", new Type[]{}, VOID);
    public static final InternalFunction __android_log_assert = new InternalFunction("__android_log_assert", new Type[]{}, VOID, Kind.SKIP);
    public static final InternalFunction __android_log_print = new InternalFunction("__android_log_print", new Type[]{}, VOID, Kind.SKIP);
    public static final InternalFunction __android_log_vprint = new InternalFunction("__android_log_vprint", new Type[]{}, VOID, Kind.SKIP);
    public static final InternalFunction __android_log_write = new InternalFunction("__android_log_write", new Type[]{}, VOID, Kind.SKIP);
    public static final InternalFunction __assert = new InternalFunction("__assert", new Type[]{}, VOID);
    public static final InternalFunction __assert2 = new InternalFunction("__assert2", new Type[]{}, VOID);
    public static final InternalFunction __atomic_cmpxchg = new InternalFunction("__atomic_cmpxchg", new Type[]{}, VOID);
    public static final InternalFunction __atomic_dec = new InternalFunction("__atomic_dec", new Type[]{}, VOID);
    public static final InternalFunction __atomic_inc = new InternalFunction("__atomic_inc", new Type[]{}, VOID);
    public static final InternalFunction __b64_ntop = new InternalFunction("__b64_ntop", new Type[]{}, VOID);
    public static final InternalFunction __cxa_atexit = new InternalFunction("__cxa_atexit", new Type[]{}, VOID);
    public static final InternalFunction __cxa_type_match = new InternalFunction("__cxa_type_match", new Type[]{}, VOID);
    public static final InternalFunction __cxa_finalize = new InternalFunction("__cxa_finalize", new Type[]{}, VOID);
    public static final InternalFunction __cxa_begin_cleanup = new InternalFunction("__cxa_begin_cleanup", new Type[]{}, VOID);
    public static final InternalFunction __cxa_guard_acquire = new InternalFunction("__cxa_guard_acquire", new Type[]{}, VOID);
    public static final InternalFunction __cxa_guard_release = new InternalFunction("__cxa_guard_release", new Type[]{}, VOID);
    public static final InternalFunction __cxa_pure_virtual = new InternalFunction("__cxa_pure_virtual", new Type[]{}, VOID);
    public static final InternalFunction __deregister_frame_info = new InternalFunction("__deregister_frame_info", new Type[]{}, VOID);
    public static final InternalFunction __register_frame_info = new InternalFunction("__register_frame_info", new Type[]{}, VOID);
    public static final InternalFunction __div0 = new InternalFunction("__div0", new Type[]{}, VOID);
    public static final InternalFunction __fork = new InternalFunction("__fork", new Type[]{}, VOID);
    public static final InternalFunction __fpclassifyd = new InternalFunction("__fpclassifyd", new Type[]{}, VOID);
    public static final InternalFunction __fpclassifyf = new InternalFunction("__fpclassifyf", new Type[]{}, VOID);
    public static final InternalFunction __futex_wait = new InternalFunction("__futex_wait", new Type[]{}, VOID);
    public static final InternalFunction __futex_wake = new InternalFunction("__futex_wake", new Type[]{}, VOID);
    public static final InternalFunction __get_h_errno = new InternalFunction("__get_h_errno", new Type[]{}, VOID);
    public static final InternalFunction __get_thread = new InternalFunction("__get_thread", new Type[]{}, VOID);
    public static final InternalFunction __gnu_Unwind_Find_exidx = new InternalFunction("__gnu_Unwind_Find_exidx", new Type[]{}, VOID);
    public static final InternalFunction __isfinite = new InternalFunction("__isfinite", new Type[]{}, VOID);
    public static final InternalFunction __isfinitef = new InternalFunction("__isfinitef", new Type[]{}, VOID);
    public static final InternalFunction __isinf = new InternalFunction("__isinf", new Type[]{}, VOID);
    public static final InternalFunction __isinff = new InternalFunction("__isinff", new Type[]{}, VOID);
    public static final InternalFunction __isnanl = new InternalFunction("__isnanl", new Type[]{}, VOID);
    public static final InternalFunction __isnormal = new InternalFunction("__isnormal", new Type[]{}, VOID);
    public static final InternalFunction __libc_init = new InternalFunction("__libc_init", new Type[]{}, VOID);
    public static final InternalFunction __open_2 = new InternalFunction("__open_2", new Type[]{}, VOID);
    public static final InternalFunction __pthread_cleanup_pop = new InternalFunction("__pthread_cleanup_pop", new Type[]{}, VOID);
    public static final InternalFunction __pthread_cleanup_push = new InternalFunction("__pthread_cleanup_push", new Type[]{}, VOID);
    public static final InternalFunction __set_tls = new InternalFunction("__set_tls", new Type[]{}, VOID);
    public static final InternalFunction __signbit = new InternalFunction("__signbit", new Type[]{}, VOID);
    public static final InternalFunction __signbitf = new InternalFunction("__signbitf", new Type[]{}, VOID);
    public static final InternalFunction __signbitl = new InternalFunction("__signbitl", new Type[]{}, VOID);
    public static final InternalFunction __srefill = new InternalFunction("__srefill", new Type[]{}, VOID);
    public static final InternalFunction __srget = new InternalFunction("__srget", new Type[]{}, VOID);
    public static final InternalFunction __strcat_chk = new InternalFunction("__strcat_chk", new Type[]{}, VOID);
    public static final InternalFunction __strcpy_chk = new InternalFunction("__strcpy_chk", new Type[]{}, VOID);
    public static final InternalFunction __strlen_chk = new InternalFunction("__strlen_chk", new Type[]{}, VOID);
    public static final InternalFunction __strchr_chk = new InternalFunction("__strchr_chk", new Type[]{}, VOID);
    public static final InternalFunction __swbuf = new InternalFunction("__swbuf", new Type[]{}, VOID);
    public static final InternalFunction __swsetup = new InternalFunction("__swsetup", new Type[]{}, VOID);
    public static final InternalFunction __system_property_find = new InternalFunction("__system_property_find", new Type[]{}, VOID);
    public static final InternalFunction __system_property_find_nth = new InternalFunction("__system_property_find_nth", new Type[]{}, VOID);
    public static final InternalFunction __system_property_get = new InternalFunction("__system_property_get", new Type[]{}, VOID);
    public static final InternalFunction __system_property_read = new InternalFunction("__system_property_read", new Type[]{}, VOID);
    public static final InternalFunction __wait4 = new InternalFunction("__wait4", new Type[]{}, VOID);
    public static final InternalFunction _exit = new InternalFunction("_exit", new Type[]{}, VOID);
    public static final InternalFunction _longjmp = new InternalFunction("_longjmp", new Type[]{ADDRESS, ADDRESS}, VOID);
    public static final InternalFunction _setjmp = new InternalFunction("_setjmp", new Type[]{ADDRESS}, VOID);
    public static final InternalFunction __FD_SET_chk = new InternalFunction("__FD_SET_chk", new Type[]{}, VOID);
    public static final InternalFunction __memcpy_chk = new InternalFunction("__memcpy_chk", new Type[]{}, VOID);
    public static final InternalFunction __sprintf_chk = new InternalFunction("__sprintf_chk", new Type[]{}, VOID);
    public static final InternalFunction accept = new InternalFunction("accept", new Type[]{}, VOID);
    public static final InternalFunction access = new InternalFunction("access", new Type[]{}, VOID);
    public static final InternalFunction acos = new InternalFunction("acos", new Type[]{}, VOID);
    public static final InternalFunction acosf = new InternalFunction("acosf", new Type[]{FLOAT}, VOID);
    public static final InternalFunction acosh = new InternalFunction("acosh", new Type[]{}, VOID);
    public static final InternalFunction acoshf = new InternalFunction("acoshf", new Type[]{}, VOID);
    public static final InternalFunction adler32 = new InternalFunction("adler32", new Type[]{}, VOID);
    public static final InternalFunction alarm = new InternalFunction("alarm", new Type[]{}, VOID);
    public static final InternalFunction alphasort = new InternalFunction("alphasort", new Type[]{}, VOID);
    public static final InternalFunction androidCreateThread = new InternalFunction("androidCreateThread", new Type[]{}, VOID);
    public static final InternalFunction androidGetTid = new InternalFunction("androidGetTid", new Type[]{}, VOID);
    public static final InternalFunction android_atomic_add = new InternalFunction("android_atomic_add", new Type[]{}, VOID);
    public static final InternalFunction android_atomic_cmpxchg = new InternalFunction("android_atomic_cmpxchg", new Type[]{}, VOID);
    public static final InternalFunction android_atomic_dec = new InternalFunction("android_atomic_dec", new Type[]{}, VOID);
    public static final InternalFunction android_atomic_inc = new InternalFunction("android_atomic_inc", new Type[]{}, VOID);
    public static final InternalFunction android_atomic_or = new InternalFunction("android_atomic_or", new Type[]{}, VOID);
    public static final InternalFunction android_atomic_release_cas = new InternalFunction("android_atomic_release_cas", new Type[]{}, VOID);
    public static final InternalFunction arc4random = new InternalFunction("arc4random", new Type[]{}, VOID);
    public static final InternalFunction arc4random_uniform = new InternalFunction("arc4random_uniform", new Type[]{}, VOID);
    public static final InternalFunction asctime = new InternalFunction("asctime", new Type[]{}, VOID);
    public static final InternalFunction ashmem_create_region = new InternalFunction("ashmem_create_region", new Type[]{}, VOID);
    public static final InternalFunction ashmem_set_prot_region = new InternalFunction("ashmem_set_prot_region", new Type[]{}, VOID);
    public static final InternalFunction asin = new InternalFunction("asin", new Type[]{}, VOID);
    public static final InternalFunction asinf = new InternalFunction("asinf", new Type[]{}, VOID);
    public static final InternalFunction asinh = new InternalFunction("asinh", new Type[]{}, VOID);
    public static final InternalFunction asinhf = new InternalFunction("asinhf", new Type[]{}, VOID);
    public static final InternalFunction atan = new InternalFunction("atan", new Type[]{}, VOID);
    public static final InternalFunction atan2 = new InternalFunction("atan2", new Type[]{}, VOID);
    public static final InternalFunction atan2f = new InternalFunction("atan2f", new Type[]{}, VOID);
    public static final InternalFunction atanf = new InternalFunction("atanf", new Type[]{}, VOID);
    public static final InternalFunction atanh = new InternalFunction("atanh", new Type[]{}, VOID);
    public static final InternalFunction atanhf = new InternalFunction("atanhf", new Type[]{}, VOID);
    public static final InternalFunction atexit = new InternalFunction("atexit", new Type[]{}, VOID);
    public static final InternalFunction atoi = new InternalFunction("atoi", new Type[]{}, VOID);
    public static final InternalFunction atol = new InternalFunction("atol", new Type[]{}, VOID);
    public static final InternalFunction atoll = new InternalFunction("atoll", new Type[]{}, VOID);
    public static final InternalFunction basename = new InternalFunction("basename", new Type[]{}, VOID);
    public static final InternalFunction bcopy = new InternalFunction("bcopy", new Type[]{}, VOID);
    public static final InternalFunction bind = new InternalFunction("bind", new Type[]{}, VOID);
    public static final InternalFunction bsd_signal = new InternalFunction("bsd_signal", new Type[]{}, VOID);
    public static final InternalFunction bsearch = new InternalFunction("bsearch", new Type[]{}, VOID);
    public static final InternalFunction btowc = new InternalFunction("btowc", new Type[]{}, VOID);
    public static final InternalFunction bzero = new InternalFunction("bzero", new Type[]{}, VOID);
    public static final InternalFunction cacheflush = new InternalFunction("cacheflush", new Type[]{}, VOID);
    public static final InternalFunction cbrt = new InternalFunction("cbrt", new Type[]{}, VOID);
    public static final InternalFunction cbrtf = new InternalFunction("cbrtf", new Type[]{}, VOID);
    public static final InternalFunction ceil = new InternalFunction("ceil", new Type[]{}, VOID);
    public static final InternalFunction ceilf = new InternalFunction("ceilf", new Type[]{}, VOID);
    public static final InternalFunction chdir = new InternalFunction("chdir", new Type[]{}, VOID);
    public static final InternalFunction chmod = new InternalFunction("chmod", new Type[]{}, VOID);
    public static final InternalFunction chown = new InternalFunction("chown", new Type[]{}, VOID);
    public static final InternalFunction clearerr = new InternalFunction("clearerr", new Type[]{}, VOID);
    public static final InternalFunction clock = new InternalFunction("clock", new Type[]{}, VOID);
    public static final InternalFunction clock_getres = new InternalFunction("clock_getres", new Type[]{}, VOID);
    public static final InternalFunction clock_gettime = new InternalFunction("clock_gettime", new Type[]{}, VOID);
    public static final InternalFunction getReplacement = new InternalFunction("getReplacement", new Type[]{}, VOID);
    public static final InternalFunction closedir = new InternalFunction("closedir", new Type[]{}, VOID);
    public static final InternalFunction closelog = new InternalFunction("closelog", new Type[]{}, VOID);
    public static final InternalFunction compress = new InternalFunction("compress", new Type[]{}, VOID);
    public static final InternalFunction compress2 = new InternalFunction("compress2", new Type[]{}, VOID);
    public static final InternalFunction compressBound = new InternalFunction("compressBound", new Type[]{}, VOID);
    public static final InternalFunction connect = new InternalFunction("connect", new Type[]{}, VOID);
    public static final InternalFunction copysign = new InternalFunction("copysign", new Type[]{}, VOID);
    public static final InternalFunction copysignf = new InternalFunction("copysignf", new Type[]{}, VOID);
    public static final InternalFunction cos = new InternalFunction("cos", new Type[]{DOUBLE}, DOUBLE);
    public static final InternalFunction cosf = new InternalFunction("cosf", new Type[]{FLOAT}, FLOAT);
    public static final InternalFunction cosh = new InternalFunction("cosh", new Type[]{}, VOID);
    public static final InternalFunction coshf = new InternalFunction("coshf", new Type[]{}, VOID);
    public static final InternalFunction crc32 = new InternalFunction("crc32", new Type[]{}, VOID);
    public static final InternalFunction creat = new InternalFunction("creat", new Type[]{}, VOID);
    public static final InternalFunction ctime = new InternalFunction("ctime", new Type[]{}, VOID);
    public static final InternalFunction ctime_r = new InternalFunction("ctime_r", new Type[]{}, VOID);
    public static final InternalFunction d2i_PKCS7 = new InternalFunction("d2i_PKCS7", new Type[]{}, VOID);
    public static final InternalFunction d2i_PKCS7_fp = new InternalFunction("d2i_PKCS7_fp", new Type[]{}, VOID);
    public static final InternalFunction d2i_RSAPrivateKey = new InternalFunction("d2i_RSAPrivateKey", new Type[]{}, VOID);
    public static final InternalFunction d2i_RSAPublicKey = new InternalFunction("d2i_RSAPublicKey", new Type[]{}, VOID);
    public static final InternalFunction d2i_X509 = new InternalFunction("d2i_X509", new Type[]{}, VOID);
    public static final InternalFunction deflate = new InternalFunction("deflate", new Type[]{}, VOID);
    public static final InternalFunction deflateBound = new InternalFunction("deflateBound", new Type[]{}, VOID);
    public static final InternalFunction deflateEnd = new InternalFunction("deflateEnd", new Type[]{}, VOID);
    public static final InternalFunction deflateInit2_ = new InternalFunction("deflateInit2_", new Type[]{}, VOID);
    public static final InternalFunction deflateInit_ = new InternalFunction("deflateInit_", new Type[]{}, VOID);
    public static final InternalFunction deflateParams = new InternalFunction("deflateParams", new Type[]{}, VOID);
    public static final InternalFunction deflateReset = new InternalFunction("deflateReset", new Type[]{}, VOID);
    public static final InternalFunction deflateSetDictionary = new InternalFunction("deflateSetDictionary", new Type[]{}, VOID);
    public static final InternalFunction difftime = new InternalFunction("difftime", new Type[]{}, VOID);
    public static final InternalFunction dirfd = new InternalFunction("dirfd", new Type[]{}, VOID);
    public static final InternalFunction dirname = new InternalFunction("dirname", new Type[]{}, VOID);
    public static final InternalFunction dirname_r = new InternalFunction("dirname_r", new Type[]{}, VOID);
    public static final InternalFunction div = new InternalFunction("div", new Type[]{}, VOID);
    public static final InternalFunction dl_unwind_find_exidx = new InternalFunction("dl_unwind_find_exidx", new Type[]{}, VOID);
    public static final InternalFunction dladdr = new InternalFunction("dladdr", new Type[]{}, VOID);
    public static final InternalFunction dlerror = new InternalFunction("dlerror", new Type[]{}, VOID);
    public static final InternalFunction dup = new InternalFunction("dup", new Type[]{}, VOID);
    public static final InternalFunction dup2 = new InternalFunction("dup2", new Type[]{}, VOID);
    public static final InternalFunction eglBindAPI = new InternalFunction("eglBindAPI", new Type[]{}, VOID);
    public static final InternalFunction eglChooseConfig = new InternalFunction("eglChooseConfig", new Type[]{}, VOID);
    public static final InternalFunction eglCreateContext = new InternalFunction("eglCreateContext", new Type[]{}, VOID);
    public static final InternalFunction eglCreateImageKHR = new InternalFunction("eglCreateImageKHR", new Type[]{}, VOID);
    public static final InternalFunction eglCreatePbufferSurface = new InternalFunction("eglCreatePbufferSurface", new Type[]{}, VOID);
    public static final InternalFunction eglCreateWindowSurface = new InternalFunction("eglCreateWindowSurface", new Type[]{}, VOID);
    public static final InternalFunction eglDestroyContext = new InternalFunction("eglDestroyContext", new Type[]{}, VOID);
    public static final InternalFunction eglDestroyImageKHR = new InternalFunction("eglDestroyImageKHR", new Type[]{}, VOID);
    public static final InternalFunction eglDestroySurface = new InternalFunction("eglDestroySurface", new Type[]{}, VOID);
    public static final InternalFunction eglGetConfigAttrib = new InternalFunction("eglGetConfigAttrib", new Type[]{}, VOID);
    public static final InternalFunction eglGetConfigs = new InternalFunction("eglGetConfigs", new Type[]{}, VOID);
    public static final InternalFunction eglGetCurrentContext = new InternalFunction("eglGetCurrentContext", new Type[]{}, VOID);
    public static final InternalFunction eglGetCurrentDisplay = new InternalFunction("eglGetCurrentDisplay", new Type[]{}, VOID);
    public static final InternalFunction eglGetCurrentSurface = new InternalFunction("eglGetCurrentSurface", new Type[]{}, VOID);
    public static final InternalFunction eglGetDisplay = new InternalFunction("eglGetDisplay", new Type[]{}, VOID);
    public static final InternalFunction eglGetError = new InternalFunction("eglGetError", new Type[]{}, VOID);
    public static final InternalFunction eglGetProcAddress = new InternalFunction("eglGetProcAddress", new Type[]{}, VOID);
    public static final InternalFunction eglInitialize = new InternalFunction("eglInitialize", new Type[]{}, VOID);
    public static final InternalFunction eglMakeCurrent = new InternalFunction("eglMakeCurrent", new Type[]{}, VOID);
    public static final InternalFunction eglQueryContext = new InternalFunction("eglQueryContext", new Type[]{}, VOID);
    public static final InternalFunction eglQueryString = new InternalFunction("eglQueryString", new Type[]{}, VOID);
    public static final InternalFunction eglQuerySurface = new InternalFunction("eglQuerySurface", new Type[]{}, VOID);
    public static final InternalFunction eglSurfaceAttrib = new InternalFunction("eglSurfaceAttrib", new Type[]{}, VOID);
    public static final InternalFunction eglSwapBuffers = new InternalFunction("eglSwapBuffers", new Type[]{}, VOID);
    public static final InternalFunction eglSwapInterval = new InternalFunction("eglSwapInterval", new Type[]{}, VOID);
    public static final InternalFunction eglTerminate = new InternalFunction("eglTerminate", new Type[]{}, VOID);
    public static final InternalFunction epoll_create = new InternalFunction("epoll_create", new Type[]{}, VOID);
    public static final InternalFunction epoll_ctl = new InternalFunction("epoll_ctl", new Type[]{}, VOID);
    public static final InternalFunction epoll_wait = new InternalFunction("epoll_wait", new Type[]{}, VOID);
    public static final InternalFunction erf = new InternalFunction("erf", new Type[]{}, VOID);
    public static final InternalFunction erfc = new InternalFunction("erfc", new Type[]{}, VOID);
    public static final InternalFunction erfcf = new InternalFunction("erfcf", new Type[]{}, VOID);
    public static final InternalFunction erff = new InternalFunction("erff", new Type[]{}, VOID);
    public static final InternalFunction execl = new InternalFunction("execl", new Type[]{}, VOID);
    public static final InternalFunction execle = new InternalFunction("execle", new Type[]{}, VOID);
    public static final InternalFunction execlp = new InternalFunction("execlp", new Type[]{}, VOID);
    public static final InternalFunction execv = new InternalFunction("execv", new Type[]{}, VOID);
    public static final InternalFunction execve = new InternalFunction("execve", new Type[]{}, VOID);
    public static final InternalFunction execvp = new InternalFunction("execvp", new Type[]{}, VOID);
    public static final InternalFunction exit = new InternalFunction("exit", new Type[]{}, VOID);
    public static final InternalFunction exp = new InternalFunction("exp", new Type[]{}, VOID);
    public static final InternalFunction exp2 = new InternalFunction("exp2", new Type[]{}, VOID);
    public static final InternalFunction exp2f = new InternalFunction("exp2f", new Type[]{}, VOID);
    public static final InternalFunction expf = new InternalFunction("expf", new Type[]{}, VOID);
    public static final InternalFunction expm1 = new InternalFunction("expm1", new Type[]{}, VOID);
    public static final InternalFunction expm1f = new InternalFunction("expm1f", new Type[]{}, VOID);
    public static final InternalFunction fabs = new InternalFunction("fabs", new Type[]{}, VOID);
    public static final InternalFunction fabsf = new InternalFunction("fabsf", new Type[]{}, VOID);
    public static final InternalFunction fchdir = new InternalFunction("fchdir", new Type[]{}, VOID);
    public static final InternalFunction fchmod = new InternalFunction("fchmod", new Type[]{}, VOID);
    public static final InternalFunction fchmodat = new InternalFunction("fchmodat", new Type[]{}, VOID);
    public static final InternalFunction fchown = new InternalFunction("fchown", new Type[]{}, VOID);
    public static final InternalFunction fcntl = new InternalFunction("fcntl", new Type[]{}, VOID);
    public static final InternalFunction fdatasync = new InternalFunction("fdatasync", new Type[]{}, VOID);
    public static final InternalFunction fdimf = new InternalFunction("fdimf", new Type[]{}, VOID);
    public static final InternalFunction fdopen = new InternalFunction("fdopen", new Type[]{}, VOID);
    public static final InternalFunction feof = new InternalFunction("feof", new Type[]{}, VOID);
    public static final InternalFunction ferror = new InternalFunction("ferror", new Type[]{}, VOID);
    public static final InternalFunction fflush = new InternalFunction("fflush", new Type[]{}, VOID);
    public static final InternalFunction fgetc = new InternalFunction("fgetc", new Type[]{}, VOID);
    public static final InternalFunction fgetpos = new InternalFunction("fgetpos", new Type[]{}, VOID);
    public static final InternalFunction fgets = new InternalFunction("fgets", new Type[]{}, VOID);
    public static final InternalFunction fileno = new InternalFunction("fileno", new Type[]{}, VOID);
    public static final InternalFunction flock = new InternalFunction("flock", new Type[]{}, VOID);
    public static final InternalFunction flockfile = new InternalFunction("flockfile", new Type[]{}, VOID);
    public static final InternalFunction floor = new InternalFunction("floor", new Type[]{}, VOID);
    public static final InternalFunction floorf = new InternalFunction("floorf", new Type[]{}, VOID);
    public static final InternalFunction fmaf = new InternalFunction("fmaf", new Type[]{}, VOID);
    public static final InternalFunction fmax = new InternalFunction("fmax", new Type[]{}, VOID);
    public static final InternalFunction fmaxf = new InternalFunction("fmaxf", new Type[]{}, VOID);
    public static final InternalFunction fmin = new InternalFunction("fmin", new Type[]{}, VOID);
    public static final InternalFunction fminf = new InternalFunction("fminf", new Type[]{}, VOID);
    public static final InternalFunction fmod = new InternalFunction("fmod", new Type[]{}, VOID);
    public static final InternalFunction fmodf = new InternalFunction("fmodf", new Type[]{}, VOID);
    public static final InternalFunction fnmatch = new InternalFunction("fnmatch", new Type[]{}, VOID);
    public static final InternalFunction fork = new InternalFunction("fork", new Type[]{}, VOID);
    public static final InternalFunction fprintf = new InternalFunction("fprintf", new Type[]{ADDRESS, FORMAT}, VOID);
    public static final InternalFunction fputc = new InternalFunction("fputc", new Type[]{}, VOID);
    public static final InternalFunction fputs = new InternalFunction("fputs", new Type[]{}, VOID);
    public static final InternalFunction fread = new InternalFunction("fread", new Type[]{ADDRESS, INT, INT, FILE}, VOID);
    public static final InternalFunction freeaddrinfo = new InternalFunction("freeaddrinfo", new Type[]{}, VOID);
    public static final InternalFunction freopen = new InternalFunction("freopen", new Type[]{}, VOID);
    public static final InternalFunction frexp = new InternalFunction("frexp", new Type[]{}, VOID);
    public static final InternalFunction frexpf = new InternalFunction("frexpf", new Type[]{}, VOID);
    public static final InternalFunction fscanf = new InternalFunction("fscanf", new Type[]{}, VOID);
    public static final InternalFunction fseek = new InternalFunction("fseek", new Type[]{FILE, INT, INT}, VOID);
    public static final InternalFunction fseeko = new InternalFunction("fseeko", new Type[]{}, VOID);
    public static final InternalFunction fsetpos = new InternalFunction("fsetpos", new Type[]{}, VOID);
    public static final InternalFunction fstat = new InternalFunction("fstat", new Type[]{}, VOID);
    public static final InternalFunction fstatat = new InternalFunction("fstatat", new Type[]{}, VOID);
    public static final InternalFunction fstatfs = new InternalFunction("fstatfs", new Type[]{}, VOID);
    public static final InternalFunction fsync = new InternalFunction("fsync", new Type[]{}, VOID);
    public static final InternalFunction ftell = new InternalFunction("ftell", new Type[]{FILE}, VOID);
    public static final InternalFunction ftello = new InternalFunction("ftello", new Type[]{}, VOID);
    public static final InternalFunction ftime = new InternalFunction("ftime", new Type[]{}, VOID);
    public static final InternalFunction ftruncate = new InternalFunction("ftruncate", new Type[]{}, VOID);
    public static final InternalFunction fts_children = new InternalFunction("fts_children", new Type[]{}, VOID);
    public static final InternalFunction fts_close = new InternalFunction("fts_close", new Type[]{}, VOID);
    public static final InternalFunction fts_open = new InternalFunction("fts_open", new Type[]{}, VOID);
    public static final InternalFunction fts_read = new InternalFunction("fts_read", new Type[]{}, VOID);
    public static final InternalFunction funlockfile = new InternalFunction("funlockfile", new Type[]{}, VOID);
    public static final InternalFunction funopen = new InternalFunction("funopen", new Type[]{}, VOID);
    public static final InternalFunction fwide = new InternalFunction("fwide", new Type[]{}, VOID);
    public static final InternalFunction fwprintf = new InternalFunction("fwprintf", new Type[]{}, VOID);
    public static final InternalFunction gai_strerror = new InternalFunction("gai_strerror", new Type[]{}, VOID);
    public static final InternalFunction get_crc_table = new InternalFunction("get_crc_table", new Type[]{}, VOID);
    public static final InternalFunction get_malloc_leak_info = new InternalFunction("get_malloc_leak_info", new Type[]{}, VOID);
    public static final InternalFunction getaddrinfo = new InternalFunction("getaddrinfo", new Type[]{}, VOID);
    public static final InternalFunction getc = new InternalFunction("getc", new Type[]{}, VOID);
    public static final InternalFunction getcwd = new InternalFunction("getcwd", new Type[]{}, VOID);
    public static final InternalFunction getdents = new InternalFunction("getdents", new Type[]{}, VOID);
    public static final InternalFunction getdtablesize = new InternalFunction("getdtablesize", new Type[]{}, VOID);
    public static final InternalFunction getegid = new InternalFunction("getegid", new Type[]{}, VOID);
    public static final InternalFunction getenv = new InternalFunction("getenv", new Type[]{STRING}, VOID);
    public static final InternalFunction geteuid = new InternalFunction("geteuid", new Type[]{}, VOID);
    public static final InternalFunction getgid = new InternalFunction("getgid", new Type[]{}, VOID);
    public static final InternalFunction getgrgid = new InternalFunction("getgrgid", new Type[]{}, VOID);
    public static final InternalFunction getgrnam = new InternalFunction("getgrnam", new Type[]{}, VOID);
    public static final InternalFunction gethostbyaddr = new InternalFunction("gethostbyaddr", new Type[]{}, VOID);
    public static final InternalFunction gethostbyname = new InternalFunction("gethostbyname", new Type[]{}, VOID);
    public static final InternalFunction gethostbyname2 = new InternalFunction("gethostbyname2", new Type[]{}, VOID);
    public static final InternalFunction gethostbyname_r = new InternalFunction("gethostbyname_r", new Type[]{}, VOID);
    public static final InternalFunction gethostname = new InternalFunction("gethostname", new Type[]{}, VOID);
    public static final InternalFunction getitimer = new InternalFunction("getitimer", new Type[]{}, VOID);
    public static final InternalFunction getlogin = new InternalFunction("getlogin", new Type[]{}, VOID);
    public static final InternalFunction getnameinfo = new InternalFunction("getnameinfo", new Type[]{}, VOID);
    public static final InternalFunction getopt = new InternalFunction("getopt", new Type[]{}, VOID);
    public static final InternalFunction getopt_long = new InternalFunction("getopt_long", new Type[]{}, VOID);
    public static final InternalFunction getpeername = new InternalFunction("getpeername", new Type[]{}, VOID);
    public static final InternalFunction getppid = new InternalFunction("getppid", new Type[]{}, VOID);
    public static final InternalFunction getpriority = new InternalFunction("getpriority", new Type[]{}, VOID);
    public static final InternalFunction getprotobyname = new InternalFunction("getprotobyname", new Type[]{}, VOID);
    public static final InternalFunction getpwnam = new InternalFunction("getpwnam", new Type[]{}, VOID);
    public static final InternalFunction getpwuid = new InternalFunction("getpwuid", new Type[]{}, VOID);
    public static final InternalFunction getresuid = new InternalFunction("getresuid", new Type[]{}, VOID);
    public static final InternalFunction getrlimit = new InternalFunction("getrlimit", new Type[]{}, VOID);
    public static final InternalFunction getrusage = new InternalFunction("getrusage", new Type[]{}, VOID);
    public static final InternalFunction getservbyname = new InternalFunction("getservbyname", new Type[]{}, VOID);
    public static final InternalFunction getsockname = new InternalFunction("getsockname", new Type[]{}, VOID);
    public static final InternalFunction getsockopt = new InternalFunction("getsockopt", new Type[]{}, VOID);
    public static final InternalFunction gettid = new InternalFunction("gettid", new Type[]{}, VOID);
    public static final InternalFunction gettimeofday = new InternalFunction("gettimeofday", new Type[]{}, VOID);
    public static final InternalFunction getuid = new InternalFunction("getuid", new Type[]{}, VOID);
    public static final InternalFunction getwc = new InternalFunction("getwc", new Type[]{}, VOID);
    public static final InternalFunction glActiveTexture = new InternalFunction("glActiveTexture", new Type[]{}, VOID);
    public static final InternalFunction glAlphaFunc = new InternalFunction("glAlphaFunc", new Type[]{}, VOID);
    public static final InternalFunction glAlphaFuncx = new InternalFunction("glAlphaFuncx", new Type[]{}, VOID);
    public static final InternalFunction glAlphaFuncxOES = new InternalFunction("glAlphaFuncxOES", new Type[]{}, VOID);
    public static final InternalFunction glAttachShader = new InternalFunction("glAttachShader", new Type[]{}, VOID);
    public static final InternalFunction glBeginPerfMonitorAMD = new InternalFunction("glBeginPerfMonitorAMD", new Type[]{}, VOID);
    public static final InternalFunction glBindAttribLocation = new InternalFunction("glBindAttribLocation", new Type[]{}, VOID);
    public static final InternalFunction glBindBuffer = new InternalFunction("glBindBuffer", new Type[]{}, VOID);
    public static final InternalFunction glBindFramebuffer = new InternalFunction("glBindFramebuffer", new Type[]{}, VOID);
    public static final InternalFunction glBindFramebufferOES = new InternalFunction("glBindFramebufferOES", new Type[]{}, VOID);
    public static final InternalFunction glBindRenderbuffer = new InternalFunction("glBindRenderbuffer", new Type[]{}, VOID);
    public static final InternalFunction glBindRenderbufferOES = new InternalFunction("glBindRenderbufferOES", new Type[]{}, VOID);
    public static final InternalFunction glBindTexture = new InternalFunction("glBindTexture", new Type[]{}, VOID);
    public static final InternalFunction glBlendColor = new InternalFunction("glBlendColor", new Type[]{}, VOID);
    public static final InternalFunction glBlendEquation = new InternalFunction("glBlendEquation", new Type[]{}, VOID);
    public static final InternalFunction glBlendEquationOES = new InternalFunction("glBlendEquationOES", new Type[]{}, VOID);
    public static final InternalFunction glBlendEquationSeparate = new InternalFunction("glBlendEquationSeparate", new Type[]{}, VOID);
    public static final InternalFunction glBlendEquationSeparateOES = new InternalFunction("glBlendEquationSeparateOES", new Type[]{}, VOID);
    public static final InternalFunction glBlendFunc = new InternalFunction("glBlendFunc", new Type[]{}, VOID);
    public static final InternalFunction glBlendFuncSeparate = new InternalFunction("glBlendFuncSeparate", new Type[]{}, VOID);
    public static final InternalFunction glBlendFuncSeparateOES = new InternalFunction("glBlendFuncSeparateOES", new Type[]{}, VOID);
    public static final InternalFunction glBufferData = new InternalFunction("glBufferData", new Type[]{}, VOID);
    public static final InternalFunction glBufferSubData = new InternalFunction("glBufferSubData", new Type[]{}, VOID);
    public static final InternalFunction glCheckFramebufferStatus = new InternalFunction("glCheckFramebufferStatus", new Type[]{}, VOID);
    public static final InternalFunction glCheckFramebufferStatusOES = new InternalFunction("glCheckFramebufferStatusOES", new Type[]{}, VOID);
    public static final InternalFunction glClear = new InternalFunction("glClear", new Type[]{}, VOID);
    public static final InternalFunction glClearColor = new InternalFunction("glClearColor", new Type[]{}, VOID);
    public static final InternalFunction glClearColorx = new InternalFunction("glClearColorx", new Type[]{}, VOID);
    public static final InternalFunction glClearColorxOES = new InternalFunction("glClearColorxOES", new Type[]{}, VOID);
    public static final InternalFunction glClearDepthf = new InternalFunction("glClearDepthf", new Type[]{}, VOID);
    public static final InternalFunction glClearDepthfOES = new InternalFunction("glClearDepthfOES", new Type[]{}, VOID);
    public static final InternalFunction glClearDepthx = new InternalFunction("glClearDepthx", new Type[]{}, VOID);
    public static final InternalFunction glClearDepthxOES = new InternalFunction("glClearDepthxOES", new Type[]{}, VOID);
    public static final InternalFunction glClearStencil = new InternalFunction("glClearStencil", new Type[]{}, VOID);
    public static final InternalFunction glClientActiveTexture = new InternalFunction("glClientActiveTexture", new Type[]{}, VOID);
    public static final InternalFunction glClipPlanef = new InternalFunction("glClipPlanef", new Type[]{}, VOID);
    public static final InternalFunction glClipPlanefOES = new InternalFunction("glClipPlanefOES", new Type[]{}, VOID);
    public static final InternalFunction glClipPlanex = new InternalFunction("glClipPlanex", new Type[]{}, VOID);
    public static final InternalFunction glClipPlanexOES = new InternalFunction("glClipPlanexOES", new Type[]{}, VOID);
    public static final InternalFunction glColor4f = new InternalFunction("glColor4f", new Type[]{}, VOID);
    public static final InternalFunction glColor4ub = new InternalFunction("glColor4ub", new Type[]{}, VOID);
    public static final InternalFunction glColor4x = new InternalFunction("glColor4x", new Type[]{}, VOID);
    public static final InternalFunction glColor4xOES = new InternalFunction("glColor4xOES", new Type[]{}, VOID);
    public static final InternalFunction glColorMask = new InternalFunction("glColorMask", new Type[]{}, VOID);
    public static final InternalFunction glColorPointer = new InternalFunction("glColorPointer", new Type[]{}, VOID);
    public static final InternalFunction glCompileShader = new InternalFunction("glCompileShader", new Type[]{}, VOID);
    public static final InternalFunction glCompressedTexImage2D = new InternalFunction("glCompressedTexImage2D", new Type[]{}, VOID);
    public static final InternalFunction glCompressedTexImage3DOES = new InternalFunction("glCompressedTexImage3DOES", new Type[]{}, VOID);
    public static final InternalFunction glCompressedTexSubImage2D = new InternalFunction("glCompressedTexSubImage2D", new Type[]{}, VOID);
    public static final InternalFunction glCompressedTexSubImage3DOES = new InternalFunction("glCompressedTexSubImage3DOES", new Type[]{}, VOID);
    public static final InternalFunction glCopyTexImage2D = new InternalFunction("glCopyTexImage2D", new Type[]{}, VOID);
    public static final InternalFunction glCopyTexSubImage2D = new InternalFunction("glCopyTexSubImage2D", new Type[]{}, VOID);
    public static final InternalFunction glCopyTexSubImage3DOES = new InternalFunction("glCopyTexSubImage3DOES", new Type[]{}, VOID);
    public static final InternalFunction glCreateProgram = new InternalFunction("glCreateProgram", new Type[]{}, VOID);
    public static final InternalFunction glCreateShader = new InternalFunction("glCreateShader", new Type[]{}, VOID);
    public static final InternalFunction glCullFace = new InternalFunction("glCullFace", new Type[]{}, VOID);
    public static final InternalFunction glCurrentPaletteMatrixOES = new InternalFunction("glCurrentPaletteMatrixOES", new Type[]{}, VOID);
    public static final InternalFunction glDeleteBuffers = new InternalFunction("glDeleteBuffers", new Type[]{}, VOID);
    public static final InternalFunction glDeleteFencesNV = new InternalFunction("glDeleteFencesNV", new Type[]{}, VOID);
    public static final InternalFunction glDeleteFramebuffers = new InternalFunction("glDeleteFramebuffers", new Type[]{}, VOID);
    public static final InternalFunction glDeleteFramebuffersOES = new InternalFunction("glDeleteFramebuffersOES", new Type[]{}, VOID);
    public static final InternalFunction glDeletePerfMonitorsAMD = new InternalFunction("glDeletePerfMonitorsAMD", new Type[]{}, VOID);
    public static final InternalFunction glDeleteProgram = new InternalFunction("glDeleteProgram", new Type[]{}, VOID);
    public static final InternalFunction glDeleteRenderbuffers = new InternalFunction("glDeleteRenderbuffers", new Type[]{}, VOID);
    public static final InternalFunction glDeleteRenderbuffersOES = new InternalFunction("glDeleteRenderbuffersOES", new Type[]{}, VOID);
    public static final InternalFunction glDeleteShader = new InternalFunction("glDeleteShader", new Type[]{}, VOID);
    public static final InternalFunction glDeleteTextures = new InternalFunction("glDeleteTextures", new Type[]{}, VOID);
    public static final InternalFunction glDepthFunc = new InternalFunction("glDepthFunc", new Type[]{}, VOID);
    public static final InternalFunction glDepthMask = new InternalFunction("glDepthMask", new Type[]{}, VOID);
    public static final InternalFunction glDepthRangef = new InternalFunction("glDepthRangef", new Type[]{}, VOID);
    public static final InternalFunction glDepthRangefOES = new InternalFunction("glDepthRangefOES", new Type[]{}, VOID);
    public static final InternalFunction glDepthRangex = new InternalFunction("glDepthRangex", new Type[]{}, VOID);
    public static final InternalFunction glDepthRangexOES = new InternalFunction("glDepthRangexOES", new Type[]{}, VOID);
    public static final InternalFunction glDetachShader = new InternalFunction("glDetachShader", new Type[]{}, VOID);
    public static final InternalFunction glDisable = new InternalFunction("glDisable", new Type[]{}, VOID);
    public static final InternalFunction glDisableClientState = new InternalFunction("glDisableClientState", new Type[]{}, VOID);
    public static final InternalFunction glDisableDriverControlQCOM = new InternalFunction("glDisableDriverControlQCOM", new Type[]{}, VOID);
    public static final InternalFunction glDisableVertexAttribArray = new InternalFunction("glDisableVertexAttribArray", new Type[]{}, VOID);
    public static final InternalFunction glDrawArrays = new InternalFunction("glDrawArrays", new Type[]{}, VOID);
    public static final InternalFunction glDrawElements = new InternalFunction("glDrawElements", new Type[]{}, VOID);
    public static final InternalFunction glDrawTexfOES = new InternalFunction("glDrawTexfOES", new Type[]{}, VOID);
    public static final InternalFunction glDrawTexfvOES = new InternalFunction("glDrawTexfvOES", new Type[]{}, VOID);
    public static final InternalFunction glDrawTexiOES = new InternalFunction("glDrawTexiOES", new Type[]{}, VOID);
    public static final InternalFunction glDrawTexivOES = new InternalFunction("glDrawTexivOES", new Type[]{}, VOID);
    public static final InternalFunction glDrawTexsOES = new InternalFunction("glDrawTexsOES", new Type[]{}, VOID);
    public static final InternalFunction glDrawTexsvOES = new InternalFunction("glDrawTexsvOES", new Type[]{}, VOID);
    public static final InternalFunction glDrawTexxOES = new InternalFunction("glDrawTexxOES", new Type[]{}, VOID);
    public static final InternalFunction glDrawTexxvOES = new InternalFunction("glDrawTexxvOES", new Type[]{}, VOID);
    public static final InternalFunction glEGLImageTargetRenderbufferStorageOES = new InternalFunction("glEGLImageTargetRenderbufferStorageOES", new Type[]{}, VOID);
    public static final InternalFunction glEGLImageTargetTexture2DOES = new InternalFunction("glEGLImageTargetTexture2DOES", new Type[]{}, VOID);
    public static final InternalFunction glEnable = new InternalFunction("glEnable", new Type[]{}, VOID);
    public static final InternalFunction glEnableClientState = new InternalFunction("glEnableClientState", new Type[]{}, VOID);
    public static final InternalFunction glEnableDriverControlQCOM = new InternalFunction("glEnableDriverControlQCOM", new Type[]{}, VOID);
    public static final InternalFunction glEnableVertexAttribArray = new InternalFunction("glEnableVertexAttribArray", new Type[]{}, VOID);
    public static final InternalFunction glEndPerfMonitorAMD = new InternalFunction("glEndPerfMonitorAMD", new Type[]{}, VOID);
    public static final InternalFunction glFinish = new InternalFunction("glFinish", new Type[]{}, VOID);
    public static final InternalFunction glFinishFenceNV = new InternalFunction("glFinishFenceNV", new Type[]{}, VOID);
    public static final InternalFunction glFlush = new InternalFunction("glFlush", new Type[]{}, VOID);
    public static final InternalFunction glFogf = new InternalFunction("glFogf", new Type[]{}, VOID);
    public static final InternalFunction glFogfv = new InternalFunction("glFogfv", new Type[]{}, VOID);
    public static final InternalFunction glFogx = new InternalFunction("glFogx", new Type[]{}, VOID);
    public static final InternalFunction glFogxOES = new InternalFunction("glFogxOES", new Type[]{}, VOID);
    public static final InternalFunction glFogxv = new InternalFunction("glFogxv", new Type[]{}, VOID);
    public static final InternalFunction glFogxvOES = new InternalFunction("glFogxvOES", new Type[]{}, VOID);
    public static final InternalFunction glFramebufferRenderbuffer = new InternalFunction("glFramebufferRenderbuffer", new Type[]{}, VOID);
    public static final InternalFunction glFramebufferRenderbufferOES = new InternalFunction("glFramebufferRenderbufferOES", new Type[]{}, VOID);
    public static final InternalFunction glFramebufferTexture2D = new InternalFunction("glFramebufferTexture2D", new Type[]{}, VOID);
    public static final InternalFunction glFramebufferTexture2DOES = new InternalFunction("glFramebufferTexture2DOES", new Type[]{}, VOID);
    public static final InternalFunction glFramebufferTexture3DOES = new InternalFunction("glFramebufferTexture3DOES", new Type[]{}, VOID);
    public static final InternalFunction glFrontFace = new InternalFunction("glFrontFace", new Type[]{}, VOID);
    public static final InternalFunction glFrustumf = new InternalFunction("glFrustumf", new Type[]{}, VOID);
    public static final InternalFunction glFrustumfOES = new InternalFunction("glFrustumfOES", new Type[]{}, VOID);
    public static final InternalFunction glFrustumx = new InternalFunction("glFrustumx", new Type[]{}, VOID);
    public static final InternalFunction glFrustumxOES = new InternalFunction("glFrustumxOES", new Type[]{}, VOID);
    public static final InternalFunction glGenBuffers = new InternalFunction("glGenBuffers", new Type[]{}, VOID);
    public static final InternalFunction glGenFencesNV = new InternalFunction("glGenFencesNV", new Type[]{}, VOID);
    public static final InternalFunction glGenFramebuffers = new InternalFunction("glGenFramebuffers", new Type[]{}, VOID);
    public static final InternalFunction glGenFramebuffersOES = new InternalFunction("glGenFramebuffersOES", new Type[]{}, VOID);
    public static final InternalFunction glGenPerfMonitorsAMD = new InternalFunction("glGenPerfMonitorsAMD", new Type[]{}, VOID);
    public static final InternalFunction glGenRenderbuffers = new InternalFunction("glGenRenderbuffers", new Type[]{}, VOID);
    public static final InternalFunction glGenRenderbuffersOES = new InternalFunction("glGenRenderbuffersOES", new Type[]{}, VOID);
    public static final InternalFunction glGenTextures = new InternalFunction("glGenTextures", new Type[]{}, VOID);
    public static final InternalFunction glGenerateMipmap = new InternalFunction("glGenerateMipmap", new Type[]{}, VOID);
    public static final InternalFunction glGenerateMipmapOES = new InternalFunction("glGenerateMipmapOES", new Type[]{}, VOID);
    public static final InternalFunction glGetActiveAttrib = new InternalFunction("glGetActiveAttrib", new Type[]{}, VOID);
    public static final InternalFunction glGetActiveUniform = new InternalFunction("glGetActiveUniform", new Type[]{}, VOID);
    public static final InternalFunction glGetAttachedShaders = new InternalFunction("glGetAttachedShaders", new Type[]{}, VOID);
    public static final InternalFunction glGetAttribLocation = new InternalFunction("glGetAttribLocation", new Type[]{}, VOID);
    public static final InternalFunction glGetBooleanv = new InternalFunction("glGetBooleanv", new Type[]{}, VOID);
    public static final InternalFunction glGetBufferParameteriv = new InternalFunction("glGetBufferParameteriv", new Type[]{}, VOID);
    public static final InternalFunction glGetBufferPointervOES = new InternalFunction("glGetBufferPointervOES", new Type[]{}, VOID);
    public static final InternalFunction glGetClipPlanef = new InternalFunction("glGetClipPlanef", new Type[]{}, VOID);
    public static final InternalFunction glGetClipPlanefOES = new InternalFunction("glGetClipPlanefOES", new Type[]{}, VOID);
    public static final InternalFunction glGetClipPlanex = new InternalFunction("glGetClipPlanex", new Type[]{}, VOID);
    public static final InternalFunction glGetClipPlanexOES = new InternalFunction("glGetClipPlanexOES", new Type[]{}, VOID);
    public static final InternalFunction glGetDriverControlStringQCOM = new InternalFunction("glGetDriverControlStringQCOM", new Type[]{}, VOID);
    public static final InternalFunction glGetDriverControlsQCOM = new InternalFunction("glGetDriverControlsQCOM", new Type[]{}, VOID);
    public static final InternalFunction glGetError = new InternalFunction("glGetError", new Type[]{}, VOID);
    public static final InternalFunction glGetFenceivNV = new InternalFunction("glGetFenceivNV", new Type[]{}, VOID);
    public static final InternalFunction glGetFixedv = new InternalFunction("glGetFixedv", new Type[]{}, VOID);
    public static final InternalFunction glGetFixedvOES = new InternalFunction("glGetFixedvOES", new Type[]{}, VOID);
    public static final InternalFunction glGetFloatv = new InternalFunction("glGetFloatv", new Type[]{}, VOID);
    public static final InternalFunction glGetFramebufferAttachmentParameteriv = new InternalFunction("glGetFramebufferAttachmentParameteriv", new Type[]{}, VOID);
    public static final InternalFunction glGetFramebufferAttachmentParameterivOES = new InternalFunction("glGetFramebufferAttachmentParameterivOES", new Type[]{}, VOID);
    public static final InternalFunction glGetIntegerv = new InternalFunction("glGetIntegerv", new Type[]{}, VOID);
    public static final InternalFunction glGetLightfv = new InternalFunction("glGetLightfv", new Type[]{}, VOID);
    public static final InternalFunction glGetLightxv = new InternalFunction("glGetLightxv", new Type[]{}, VOID);
    public static final InternalFunction glGetLightxvOES = new InternalFunction("glGetLightxvOES", new Type[]{}, VOID);
    public static final InternalFunction glGetMaterialfv = new InternalFunction("glGetMaterialfv", new Type[]{}, VOID);
    public static final InternalFunction glGetMaterialxv = new InternalFunction("glGetMaterialxv", new Type[]{}, VOID);
    public static final InternalFunction glGetMaterialxvOES = new InternalFunction("glGetMaterialxvOES", new Type[]{}, VOID);
    public static final InternalFunction glGetPerfMonitorCounterDataAMD = new InternalFunction("glGetPerfMonitorCounterDataAMD", new Type[]{}, VOID);
    public static final InternalFunction glGetPerfMonitorCounterInfoAMD = new InternalFunction("glGetPerfMonitorCounterInfoAMD", new Type[]{}, VOID);
    public static final InternalFunction glGetPerfMonitorCounterStringAMD = new InternalFunction("glGetPerfMonitorCounterStringAMD", new Type[]{}, VOID);
    public static final InternalFunction glGetPerfMonitorCountersAMD = new InternalFunction("glGetPerfMonitorCountersAMD", new Type[]{}, VOID);
    public static final InternalFunction glGetPerfMonitorGroupStringAMD = new InternalFunction("glGetPerfMonitorGroupStringAMD", new Type[]{}, VOID);
    public static final InternalFunction glGetPerfMonitorGroupsAMD = new InternalFunction("glGetPerfMonitorGroupsAMD", new Type[]{}, VOID);
    public static final InternalFunction glGetPointerv = new InternalFunction("glGetPointerv", new Type[]{}, VOID);
    public static final InternalFunction glGetProgramBinaryOES = new InternalFunction("glGetProgramBinaryOES", new Type[]{}, VOID);
    public static final InternalFunction glGetProgramInfoLog = new InternalFunction("glGetProgramInfoLog", new Type[]{}, VOID);
    public static final InternalFunction glGetProgramiv = new InternalFunction("glGetProgramiv", new Type[]{}, VOID);
    public static final InternalFunction glGetRenderbufferParameteriv = new InternalFunction("glGetRenderbufferParameteriv", new Type[]{}, VOID);
    public static final InternalFunction glGetRenderbufferParameterivOES = new InternalFunction("glGetRenderbufferParameterivOES", new Type[]{}, VOID);
    public static final InternalFunction glGetShaderInfoLog = new InternalFunction("glGetShaderInfoLog", new Type[]{}, VOID);
    public static final InternalFunction glGetShaderPrecisionFormat = new InternalFunction("glGetShaderPrecisionFormat", new Type[]{}, VOID);
    public static final InternalFunction glGetShaderSource = new InternalFunction("glGetShaderSource", new Type[]{}, VOID);
    public static final InternalFunction glGetShaderiv = new InternalFunction("glGetShaderiv", new Type[]{}, VOID);
    public static final InternalFunction glGetString = new InternalFunction("glGetString", new Type[]{}, VOID);
    public static final InternalFunction glGetTexEnvfv = new InternalFunction("glGetTexEnvfv", new Type[]{}, VOID);
    public static final InternalFunction glGetTexEnviv = new InternalFunction("glGetTexEnviv", new Type[]{}, VOID);
    public static final InternalFunction glGetTexEnvxv = new InternalFunction("glGetTexEnvxv", new Type[]{}, VOID);
    public static final InternalFunction glGetTexEnvxvOES = new InternalFunction("glGetTexEnvxvOES", new Type[]{}, VOID);
    public static final InternalFunction glGetTexGenfvOES = new InternalFunction("glGetTexGenfvOES", new Type[]{}, VOID);
    public static final InternalFunction glGetTexGenivOES = new InternalFunction("glGetTexGenivOES", new Type[]{}, VOID);
    public static final InternalFunction glGetTexGenxvOES = new InternalFunction("glGetTexGenxvOES", new Type[]{}, VOID);
    public static final InternalFunction glGetTexParameterfv = new InternalFunction("glGetTexParameterfv", new Type[]{}, VOID);
    public static final InternalFunction glGetTexParameteriv = new InternalFunction("glGetTexParameteriv", new Type[]{}, VOID);
    public static final InternalFunction glGetTexParameterxv = new InternalFunction("glGetTexParameterxv", new Type[]{}, VOID);
    public static final InternalFunction glGetTexParameterxvOES = new InternalFunction("glGetTexParameterxvOES", new Type[]{}, VOID);
    public static final InternalFunction glGetUniformLocation = new InternalFunction("glGetUniformLocation", new Type[]{}, VOID);
    public static final InternalFunction glGetUniformfv = new InternalFunction("glGetUniformfv", new Type[]{}, VOID);
    public static final InternalFunction glGetUniformiv = new InternalFunction("glGetUniformiv", new Type[]{}, VOID);
    public static final InternalFunction glGetVertexAttribPointerv = new InternalFunction("glGetVertexAttribPointerv", new Type[]{}, VOID);
    public static final InternalFunction glGetVertexAttribfv = new InternalFunction("glGetVertexAttribfv", new Type[]{}, VOID);
    public static final InternalFunction glGetVertexAttribiv = new InternalFunction("glGetVertexAttribiv", new Type[]{}, VOID);
    public static final InternalFunction glHint = new InternalFunction("glHint", new Type[]{}, VOID);
    public static final InternalFunction glIsBuffer = new InternalFunction("glIsBuffer", new Type[]{}, VOID);
    public static final InternalFunction glIsEnabled = new InternalFunction("glIsEnabled", new Type[]{}, VOID);
    public static final InternalFunction glIsFenceNV = new InternalFunction("glIsFenceNV", new Type[]{}, VOID);
    public static final InternalFunction glIsFramebuffer = new InternalFunction("glIsFramebuffer", new Type[]{}, VOID);
    public static final InternalFunction glIsFramebufferOES = new InternalFunction("glIsFramebufferOES", new Type[]{}, VOID);
    public static final InternalFunction glIsProgram = new InternalFunction("glIsProgram", new Type[]{}, VOID);
    public static final InternalFunction glIsRenderbuffer = new InternalFunction("glIsRenderbuffer", new Type[]{}, VOID);
    public static final InternalFunction glIsRenderbufferOES = new InternalFunction("glIsRenderbufferOES", new Type[]{}, VOID);
    public static final InternalFunction glIsShader = new InternalFunction("glIsShader", new Type[]{}, VOID);
    public static final InternalFunction glIsTexture = new InternalFunction("glIsTexture", new Type[]{}, VOID);
    public static final InternalFunction glLightModelf = new InternalFunction("glLightModelf", new Type[]{}, VOID);
    public static final InternalFunction glLightModelfv = new InternalFunction("glLightModelfv", new Type[]{}, VOID);
    public static final InternalFunction glLightModelx = new InternalFunction("glLightModelx", new Type[]{}, VOID);
    public static final InternalFunction glLightModelxOES = new InternalFunction("glLightModelxOES", new Type[]{}, VOID);
    public static final InternalFunction glLightModelxv = new InternalFunction("glLightModelxv", new Type[]{}, VOID);
    public static final InternalFunction glLightModelxvOES = new InternalFunction("glLightModelxvOES", new Type[]{}, VOID);
    public static final InternalFunction glLightf = new InternalFunction("glLightf", new Type[]{}, VOID);
    public static final InternalFunction glLightfv = new InternalFunction("glLightfv", new Type[]{}, VOID);
    public static final InternalFunction glLightx = new InternalFunction("glLightx", new Type[]{}, VOID);
    public static final InternalFunction glLightxOES = new InternalFunction("glLightxOES", new Type[]{}, VOID);
    public static final InternalFunction glLightxv = new InternalFunction("glLightxv", new Type[]{}, VOID);
    public static final InternalFunction glLightxvOES = new InternalFunction("glLightxvOES", new Type[]{}, VOID);
    public static final InternalFunction glLineWidth = new InternalFunction("glLineWidth", new Type[]{}, VOID);
    public static final InternalFunction glLineWidthx = new InternalFunction("glLineWidthx", new Type[]{}, VOID);
    public static final InternalFunction glLineWidthxOES = new InternalFunction("glLineWidthxOES", new Type[]{}, VOID);
    public static final InternalFunction glLinkProgram = new InternalFunction("glLinkProgram", new Type[]{}, VOID);
    public static final InternalFunction glLoadIdentity = new InternalFunction("glLoadIdentity", new Type[]{}, VOID);
    public static final InternalFunction glLoadMatrixf = new InternalFunction("glLoadMatrixf", new Type[]{}, VOID);
    public static final InternalFunction glLoadMatrixx = new InternalFunction("glLoadMatrixx", new Type[]{}, VOID);
    public static final InternalFunction glLoadMatrixxOES = new InternalFunction("glLoadMatrixxOES", new Type[]{}, VOID);
    public static final InternalFunction glLoadPaletteFromModelViewMatrixOES = new InternalFunction("glLoadPaletteFromModelViewMatrixOES", new Type[]{}, VOID);
    public static final InternalFunction glLogicOp = new InternalFunction("glLogicOp", new Type[]{}, VOID);
    public static final InternalFunction glMapBufferOES = new InternalFunction("glMapBufferOES", new Type[]{}, VOID);
    public static final InternalFunction glMaterialf = new InternalFunction("glMaterialf", new Type[]{}, VOID);
    public static final InternalFunction glMaterialfv = new InternalFunction("glMaterialfv", new Type[]{}, VOID);
    public static final InternalFunction glMaterialx = new InternalFunction("glMaterialx", new Type[]{}, VOID);
    public static final InternalFunction glMaterialxOES = new InternalFunction("glMaterialxOES", new Type[]{}, VOID);
    public static final InternalFunction glMaterialxv = new InternalFunction("glMaterialxv", new Type[]{}, VOID);
    public static final InternalFunction glMaterialxvOES = new InternalFunction("glMaterialxvOES", new Type[]{}, VOID);
    public static final InternalFunction glMatrixIndexPointerOES = new InternalFunction("glMatrixIndexPointerOES", new Type[]{}, VOID);
    public static final InternalFunction glMatrixMode = new InternalFunction("glMatrixMode", new Type[]{}, VOID);
    public static final InternalFunction glMultMatrixf = new InternalFunction("glMultMatrixf", new Type[]{}, VOID);
    public static final InternalFunction glMultMatrixx = new InternalFunction("glMultMatrixx", new Type[]{}, VOID);
    public static final InternalFunction glMultMatrixxOES = new InternalFunction("glMultMatrixxOES", new Type[]{}, VOID);
    public static final InternalFunction glMultiTexCoord4f = new InternalFunction("glMultiTexCoord4f", new Type[]{}, VOID);
    public static final InternalFunction glMultiTexCoord4x = new InternalFunction("glMultiTexCoord4x", new Type[]{}, VOID);
    public static final InternalFunction glMultiTexCoord4xOES = new InternalFunction("glMultiTexCoord4xOES", new Type[]{}, VOID);
    public static final InternalFunction glNormal3f = new InternalFunction("glNormal3f", new Type[]{}, VOID);
    public static final InternalFunction glNormal3x = new InternalFunction("glNormal3x", new Type[]{}, VOID);
    public static final InternalFunction glNormal3xOES = new InternalFunction("glNormal3xOES", new Type[]{}, VOID);
    public static final InternalFunction glNormalPointer = new InternalFunction("glNormalPointer", new Type[]{}, VOID);
    public static final InternalFunction glOrthof = new InternalFunction("glOrthof", new Type[]{}, VOID);
    public static final InternalFunction glOrthofOES = new InternalFunction("glOrthofOES", new Type[]{}, VOID);
    public static final InternalFunction glOrthox = new InternalFunction("glOrthox", new Type[]{}, VOID);
    public static final InternalFunction glOrthoxOES = new InternalFunction("glOrthoxOES", new Type[]{}, VOID);
    public static final InternalFunction glPixelStorei = new InternalFunction("glPixelStorei", new Type[]{}, VOID);
    public static final InternalFunction glPointParameterf = new InternalFunction("glPointParameterf", new Type[]{}, VOID);
    public static final InternalFunction glPointParameterfv = new InternalFunction("glPointParameterfv", new Type[]{}, VOID);
    public static final InternalFunction glPointParameterx = new InternalFunction("glPointParameterx", new Type[]{}, VOID);
    public static final InternalFunction glPointParameterxOES = new InternalFunction("glPointParameterxOES", new Type[]{}, VOID);
    public static final InternalFunction glPointParameterxv = new InternalFunction("glPointParameterxv", new Type[]{}, VOID);
    public static final InternalFunction glPointParameterxvOES = new InternalFunction("glPointParameterxvOES", new Type[]{}, VOID);
    public static final InternalFunction glPointSize = new InternalFunction("glPointSize", new Type[]{}, VOID);
    public static final InternalFunction glPointSizePointerOES = new InternalFunction("glPointSizePointerOES", new Type[]{}, VOID);
    public static final InternalFunction glPointSizex = new InternalFunction("glPointSizex", new Type[]{}, VOID);
    public static final InternalFunction glPointSizexOES = new InternalFunction("glPointSizexOES", new Type[]{}, VOID);
    public static final InternalFunction glPolygonOffset = new InternalFunction("glPolygonOffset", new Type[]{}, VOID);
    public static final InternalFunction glPolygonOffsetx = new InternalFunction("glPolygonOffsetx", new Type[]{}, VOID);
    public static final InternalFunction glPolygonOffsetxOES = new InternalFunction("glPolygonOffsetxOES", new Type[]{}, VOID);
    public static final InternalFunction glPopMatrix = new InternalFunction("glPopMatrix", new Type[]{}, VOID);
    public static final InternalFunction glProgramBinaryOES = new InternalFunction("glProgramBinaryOES", new Type[]{}, VOID);
    public static final InternalFunction glPushMatrix = new InternalFunction("glPushMatrix", new Type[]{}, VOID);
    public static final InternalFunction glQueryMatrixxOES = new InternalFunction("glQueryMatrixxOES", new Type[]{}, VOID);
    public static final InternalFunction glReadPixels = new InternalFunction("glReadPixels", new Type[]{INT, INT, INT, INT, INT, INT, ADDRESS}, VOID);
    public static final InternalFunction glReleaseShaderCompiler = new InternalFunction("glReleaseShaderCompiler", new Type[]{}, VOID);
    public static final InternalFunction glRenderbufferStorage = new InternalFunction("glRenderbufferStorage", new Type[]{}, VOID);
    public static final InternalFunction glRenderbufferStorageOES = new InternalFunction("glRenderbufferStorageOES", new Type[]{}, VOID);
    public static final InternalFunction glRotatef = new InternalFunction("glRotatef", new Type[]{}, VOID);
    public static final InternalFunction glRotatex = new InternalFunction("glRotatex", new Type[]{}, VOID);
    public static final InternalFunction glRotatexOES = new InternalFunction("glRotatexOES", new Type[]{}, VOID);
    public static final InternalFunction glSampleCoverage = new InternalFunction("glSampleCoverage", new Type[]{}, VOID);
    public static final InternalFunction glSampleCoveragex = new InternalFunction("glSampleCoveragex", new Type[]{}, VOID);
    public static final InternalFunction glSampleCoveragexOES = new InternalFunction("glSampleCoveragexOES", new Type[]{}, VOID);
    public static final InternalFunction glScalef = new InternalFunction("glScalef", new Type[]{}, VOID);
    public static final InternalFunction glScalex = new InternalFunction("glScalex", new Type[]{}, VOID);
    public static final InternalFunction glScalexOES = new InternalFunction("glScalexOES", new Type[]{}, VOID);
    public static final InternalFunction glScissor = new InternalFunction("glScissor", new Type[]{}, VOID);
    public static final InternalFunction glSelectPerfMonitorCountersAMD = new InternalFunction("glSelectPerfMonitorCountersAMD", new Type[]{}, VOID);
    public static final InternalFunction glSetFenceNV = new InternalFunction("glSetFenceNV", new Type[]{}, VOID);
    public static final InternalFunction glShadeModel = new InternalFunction("glShadeModel", new Type[]{}, VOID);
    public static final InternalFunction glShaderBinary = new InternalFunction("glShaderBinary", new Type[]{}, VOID);
    public static final InternalFunction glShaderSource = new InternalFunction("glShaderSource", new Type[]{}, VOID);
    public static final InternalFunction glStencilFunc = new InternalFunction("glStencilFunc", new Type[]{}, VOID);
    public static final InternalFunction glStencilFuncSeparate = new InternalFunction("glStencilFuncSeparate", new Type[]{}, VOID);
    public static final InternalFunction glStencilMask = new InternalFunction("glStencilMask", new Type[]{}, VOID);
    public static final InternalFunction glStencilMaskSeparate = new InternalFunction("glStencilMaskSeparate", new Type[]{}, VOID);
    public static final InternalFunction glStencilOp = new InternalFunction("glStencilOp", new Type[]{}, VOID);
    public static final InternalFunction glStencilOpSeparate = new InternalFunction("glStencilOpSeparate", new Type[]{}, VOID);
    public static final InternalFunction glTestFenceNV = new InternalFunction("glTestFenceNV", new Type[]{}, VOID);
    public static final InternalFunction glTexCoordPointer = new InternalFunction("glTexCoordPointer", new Type[]{}, VOID);
    public static final InternalFunction glTexEnvf = new InternalFunction("glTexEnvf", new Type[]{}, VOID);
    public static final InternalFunction glTexEnvfv = new InternalFunction("glTexEnvfv", new Type[]{}, VOID);
    public static final InternalFunction glTexEnvi = new InternalFunction("glTexEnvi", new Type[]{}, VOID);
    public static final InternalFunction glTexEnviv = new InternalFunction("glTexEnviv", new Type[]{}, VOID);
    public static final InternalFunction glTexEnvx = new InternalFunction("glTexEnvx", new Type[]{}, VOID);
    public static final InternalFunction glTexEnvxOES = new InternalFunction("glTexEnvxOES", new Type[]{}, VOID);
    public static final InternalFunction glTexEnvxv = new InternalFunction("glTexEnvxv", new Type[]{}, VOID);
    public static final InternalFunction glTexEnvxvOES = new InternalFunction("glTexEnvxvOES", new Type[]{}, VOID);
    public static final InternalFunction glTexGenfOES = new InternalFunction("glTexGenfOES", new Type[]{}, VOID);
    public static final InternalFunction glTexGenfvOES = new InternalFunction("glTexGenfvOES", new Type[]{}, VOID);
    public static final InternalFunction glTexGeniOES = new InternalFunction("glTexGeniOES", new Type[]{}, VOID);
    public static final InternalFunction glTexGenivOES = new InternalFunction("glTexGenivOES", new Type[]{}, VOID);
    public static final InternalFunction glTexGenxOES = new InternalFunction("glTexGenxOES", new Type[]{}, VOID);
    public static final InternalFunction glTexGenxvOES = new InternalFunction("glTexGenxvOES", new Type[]{}, VOID);
    public static final InternalFunction glTexImage2D = new InternalFunction("glTexImage2D", new Type[]{}, VOID);
    public static final InternalFunction glTexImage3DOES = new InternalFunction("glTexImage3DOES", new Type[]{}, VOID);
    public static final InternalFunction glTexParameterf = new InternalFunction("glTexParameterf", new Type[]{}, VOID);
    public static final InternalFunction glTexParameterfv = new InternalFunction("glTexParameterfv", new Type[]{}, VOID);
    public static final InternalFunction glTexParameteri = new InternalFunction("glTexParameteri", new Type[]{}, VOID);
    public static final InternalFunction glTexParameteriv = new InternalFunction("glTexParameteriv", new Type[]{}, VOID);
    public static final InternalFunction glTexParameterx = new InternalFunction("glTexParameterx", new Type[]{}, VOID);
    public static final InternalFunction glTexParameterxOES = new InternalFunction("glTexParameterxOES", new Type[]{}, VOID);
    public static final InternalFunction glTexParameterxv = new InternalFunction("glTexParameterxv", new Type[]{}, VOID);
    public static final InternalFunction glTexParameterxvOES = new InternalFunction("glTexParameterxvOES", new Type[]{}, VOID);
    public static final InternalFunction glTexSubImage2D = new InternalFunction("glTexSubImage2D", new Type[]{}, VOID);
    public static final InternalFunction glTexSubImage3DOES = new InternalFunction("glTexSubImage3DOES", new Type[]{}, VOID);
    public static final InternalFunction glTranslatef = new InternalFunction("glTranslatef", new Type[]{}, VOID);
    public static final InternalFunction glTranslatex = new InternalFunction("glTranslatex", new Type[]{}, VOID);
    public static final InternalFunction glTranslatexOES = new InternalFunction("glTranslatexOES", new Type[]{}, VOID);
    public static final InternalFunction glUniform1f = new InternalFunction("glUniform1f", new Type[]{}, VOID);
    public static final InternalFunction glUniform1fv = new InternalFunction("glUniform1fv", new Type[]{}, VOID);
    public static final InternalFunction glUniform1i = new InternalFunction("glUniform1i", new Type[]{}, VOID);
    public static final InternalFunction glUniform1iv = new InternalFunction("glUniform1iv", new Type[]{}, VOID);
    public static final InternalFunction glUniform2f = new InternalFunction("glUniform2f", new Type[]{}, VOID);
    public static final InternalFunction glUniform2fv = new InternalFunction("glUniform2fv", new Type[]{}, VOID);
    public static final InternalFunction glUniform2i = new InternalFunction("glUniform2i", new Type[]{}, VOID);
    public static final InternalFunction glUniform2iv = new InternalFunction("glUniform2iv", new Type[]{}, VOID);
    public static final InternalFunction glUniform3f = new InternalFunction("glUniform3f", new Type[]{}, VOID);
    public static final InternalFunction glUniform3fv = new InternalFunction("glUniform3fv", new Type[]{}, VOID);
    public static final InternalFunction glUniform3i = new InternalFunction("glUniform3i", new Type[]{}, VOID);
    public static final InternalFunction glUniform3iv = new InternalFunction("glUniform3iv", new Type[]{}, VOID);
    public static final InternalFunction glUniform4f = new InternalFunction("glUniform4f", new Type[]{}, VOID);
    public static final InternalFunction glUniform4fv = new InternalFunction("glUniform4fv", new Type[]{}, VOID);
    public static final InternalFunction glUniform4i = new InternalFunction("glUniform4i", new Type[]{}, VOID);
    public static final InternalFunction glUniform4iv = new InternalFunction("glUniform4iv", new Type[]{}, VOID);
    public static final InternalFunction glUniformMatrix2fv = new InternalFunction("glUniformMatrix2fv", new Type[]{}, VOID);
    public static final InternalFunction glUniformMatrix3fv = new InternalFunction("glUniformMatrix3fv", new Type[]{}, VOID);
    public static final InternalFunction glUniformMatrix4fv = new InternalFunction("glUniformMatrix4fv", new Type[]{}, VOID);
    public static final InternalFunction glUnmapBufferOES = new InternalFunction("glUnmapBufferOES", new Type[]{}, VOID);
    public static final InternalFunction glUseProgram = new InternalFunction("glUseProgram", new Type[]{}, VOID);
    public static final InternalFunction glValidateProgram = new InternalFunction("glValidateProgram", new Type[]{}, VOID);
    public static final InternalFunction glVertexAttrib1f = new InternalFunction("glVertexAttrib1f", new Type[]{}, VOID);
    public static final InternalFunction glVertexAttrib1fv = new InternalFunction("glVertexAttrib1fv", new Type[]{}, VOID);
    public static final InternalFunction glVertexAttrib2f = new InternalFunction("glVertexAttrib2f", new Type[]{}, VOID);
    public static final InternalFunction glVertexAttrib2fv = new InternalFunction("glVertexAttrib2fv", new Type[]{}, VOID);
    public static final InternalFunction glVertexAttrib3f = new InternalFunction("glVertexAttrib3f", new Type[]{}, VOID);
    public static final InternalFunction glVertexAttrib3fv = new InternalFunction("glVertexAttrib3fv", new Type[]{}, VOID);
    public static final InternalFunction glVertexAttrib4f = new InternalFunction("glVertexAttrib4f", new Type[]{}, VOID);
    public static final InternalFunction glVertexAttrib4fv = new InternalFunction("glVertexAttrib4fv", new Type[]{}, VOID);
    public static final InternalFunction glVertexAttribPointer = new InternalFunction("glVertexAttribPointer", new Type[]{}, VOID);
    public static final InternalFunction glVertexPointer = new InternalFunction("glVertexPointer", new Type[]{}, VOID);
    public static final InternalFunction glViewport = new InternalFunction("glViewport", new Type[]{}, VOID);
    public static final InternalFunction glWeightPointerOES = new InternalFunction("glWeightPointerOES", new Type[]{}, VOID);
    public static final InternalFunction gmtime = new InternalFunction("gmtime", new Type[]{}, VOID);
    public static final InternalFunction gmtime64_r = new InternalFunction("gmtime64_r", new Type[]{}, VOID);
    public static final InternalFunction gmtime_r = new InternalFunction("gmtime_r", new Type[]{}, VOID);
    public static final InternalFunction gzclose = new InternalFunction("gzclose", new Type[]{}, VOID);
    public static final InternalFunction gzeof = new InternalFunction("gzeof", new Type[]{}, VOID);
    public static final InternalFunction gzerror = new InternalFunction("gzerror", new Type[]{}, VOID);
    public static final InternalFunction gzgets = new InternalFunction("gzgets", new Type[]{}, VOID);
    public static final InternalFunction gzopen = new InternalFunction("gzopen", new Type[]{}, VOID);
    public static final InternalFunction gzputs = new InternalFunction("gzputs", new Type[]{}, VOID);
    public static final InternalFunction gzread = new InternalFunction("gzread", new Type[]{}, VOID);
    public static final InternalFunction gzrewind = new InternalFunction("gzrewind", new Type[]{}, VOID);
    public static final InternalFunction gzwrite = new InternalFunction("gzwrite", new Type[]{}, VOID);
    public static final InternalFunction hstrerror = new InternalFunction("hstrerror", new Type[]{}, VOID);
    public static final InternalFunction hypot = new InternalFunction("hypot", new Type[]{}, VOID);
    public static final InternalFunction hypotf = new InternalFunction("hypotf", new Type[]{}, VOID);
    public static final InternalFunction i2a_ASN1_OBJECT = new InternalFunction("i2a_ASN1_OBJECT", new Type[]{}, VOID);
    public static final InternalFunction i2d_PublicKey = new InternalFunction("i2d_PublicKey", new Type[]{}, VOID);
    public static final InternalFunction i2d_RSAPrivateKey = new InternalFunction("i2d_RSAPrivateKey", new Type[]{}, VOID);
    public static final InternalFunction i2d_RSAPublicKey = new InternalFunction("i2d_RSAPublicKey", new Type[]{}, VOID);
    public static final InternalFunction if_indextoname = new InternalFunction("if_indextoname", new Type[]{}, VOID);
    public static final InternalFunction if_nametoindex = new InternalFunction("if_nametoindex", new Type[]{}, VOID);
    public static final InternalFunction ilogbf = new InternalFunction("ilogbf", new Type[]{}, VOID);
    public static final InternalFunction index = new InternalFunction("index", new Type[]{}, VOID);
    public static final InternalFunction inet_addr = new InternalFunction("inet_addr", new Type[]{}, VOID);
    public static final InternalFunction inet_aton = new InternalFunction("inet_aton", new Type[]{}, VOID);
    public static final InternalFunction inet_nsap_addr = new InternalFunction("inet_nsap_addr", new Type[]{}, VOID);
    public static final InternalFunction inet_nsap_ntoa = new InternalFunction("inet_nsap_ntoa", new Type[]{}, VOID);
    public static final InternalFunction inet_ntoa = new InternalFunction("inet_ntoa", new Type[]{}, VOID);
    public static final InternalFunction inet_ntop = new InternalFunction("inet_ntop", new Type[]{}, VOID);
    public static final InternalFunction inet_pton = new InternalFunction("inet_pton", new Type[]{}, VOID);
    public static final InternalFunction inflate = new InternalFunction("inflate", new Type[]{}, VOID);
    public static final InternalFunction inflateCopy = new InternalFunction("inflateCopy", new Type[]{}, VOID);
    public static final InternalFunction inflateEnd = new InternalFunction("inflateEnd", new Type[]{}, VOID);
    public static final InternalFunction inflateInit2_ = new InternalFunction("inflateInit2_", new Type[]{}, VOID);
    public static final InternalFunction inflateInit_ = new InternalFunction("inflateInit_", new Type[]{}, VOID);
    public static final InternalFunction inflateReset = new InternalFunction("inflateReset", new Type[]{}, VOID);
    public static final InternalFunction inflateSetDictionary = new InternalFunction("inflateSetDictionary", new Type[]{}, VOID);
    public static final InternalFunction inflateSync = new InternalFunction("inflateSync", new Type[]{}, VOID);
    public static final InternalFunction initgroups = new InternalFunction("initgroups", new Type[]{}, VOID);
    public static final InternalFunction inotify_add_watch = new InternalFunction("inotify_add_watch", new Type[]{}, VOID);
    public static final InternalFunction inotify_init = new InternalFunction("inotify_init", new Type[]{}, VOID);
    public static final InternalFunction inotify_rm_watch = new InternalFunction("inotify_rm_watch", new Type[]{}, VOID);
    public static final InternalFunction isalnum = new InternalFunction("isalnum", new Type[]{}, VOID);
    public static final InternalFunction isalpha = new InternalFunction("isalpha", new Type[]{}, VOID);
    public static final InternalFunction isatty = new InternalFunction("isatty", new Type[]{}, VOID);
    public static final InternalFunction iscntrl = new InternalFunction("iscntrl", new Type[]{}, VOID);
    public static final InternalFunction isdigit = new InternalFunction("isdigit", new Type[]{}, VOID);
    public static final InternalFunction isgraph = new InternalFunction("isgraph", new Type[]{}, VOID);
    public static final InternalFunction islower = new InternalFunction("islower", new Type[]{}, VOID);
    public static final InternalFunction isnan = new InternalFunction("isnan", new Type[]{}, VOID);
    public static final InternalFunction isnanf = new InternalFunction("isnanf", new Type[]{}, VOID);
    public static final InternalFunction isprint = new InternalFunction("isprint", new Type[]{}, VOID);
    public static final InternalFunction ispunct = new InternalFunction("ispunct", new Type[]{}, VOID);
    public static final InternalFunction issetugid = new InternalFunction("issetugid", new Type[]{}, VOID);
    public static final InternalFunction isspace = new InternalFunction("isspace", new Type[]{}, VOID);
    public static final InternalFunction isupper = new InternalFunction("isupper", new Type[]{}, VOID);
    public static final InternalFunction iswalnum = new InternalFunction("iswalnum", new Type[]{}, VOID);
    public static final InternalFunction iswalpha = new InternalFunction("iswalpha", new Type[]{}, VOID);
    public static final InternalFunction iswcntrl = new InternalFunction("iswcntrl", new Type[]{}, VOID);
    public static final InternalFunction iswctype = new InternalFunction("iswctype", new Type[]{}, VOID);
    public static final InternalFunction iswdigit = new InternalFunction("iswdigit", new Type[]{}, VOID);
    public static final InternalFunction iswlower = new InternalFunction("iswlower", new Type[]{}, VOID);
    public static final InternalFunction iswprint = new InternalFunction("iswprint", new Type[]{}, VOID);
    public static final InternalFunction iswpunct = new InternalFunction("iswpunct", new Type[]{}, VOID);
    public static final InternalFunction iswspace = new InternalFunction("iswspace", new Type[]{}, VOID);
    public static final InternalFunction iswupper = new InternalFunction("iswupper", new Type[]{}, VOID);
    public static final InternalFunction iswxdigit = new InternalFunction("iswxdigit", new Type[]{}, VOID);
    public static final InternalFunction isxdigit = new InternalFunction("isxdigit", new Type[]{}, VOID);
    public static final InternalFunction jniThrowException = new InternalFunction("jniThrowException", new Type[]{}, VOID);
    public static final InternalFunction jrand48 = new InternalFunction("jrand48", new Type[]{}, VOID);
    public static final InternalFunction kill = new InternalFunction("kill", new Type[]{}, VOID);
    public static final InternalFunction klogctl = new InternalFunction("klogctl", new Type[]{}, VOID);
    public static final InternalFunction lchown = new InternalFunction("lchown", new Type[]{}, VOID);
    public static final InternalFunction ldexp = new InternalFunction("ldexp", new Type[]{}, VOID);
    public static final InternalFunction ldexpf = new InternalFunction("ldexpf", new Type[]{}, VOID);
    public static final InternalFunction ldiv = new InternalFunction("ldiv", new Type[]{}, VOID);
    public static final InternalFunction lgamma = new InternalFunction("lgamma", new Type[]{}, VOID);
    public static final InternalFunction lgammaf = new InternalFunction("lgammaf", new Type[]{}, VOID);
    public static final InternalFunction lgammaf_r = new InternalFunction("lgammaf_r", new Type[]{}, VOID);
    public static final InternalFunction link = new InternalFunction("link", new Type[]{}, VOID);
    public static final InternalFunction listen = new InternalFunction("listen", new Type[]{}, VOID);
    public static final InternalFunction llrint = new InternalFunction("llrint", new Type[]{}, VOID);
    public static final InternalFunction llrintf = new InternalFunction("llrintf", new Type[]{}, VOID);
    public static final InternalFunction localtime = new InternalFunction("localtime", new Type[]{}, VOID);
    public static final InternalFunction localtime64_r = new InternalFunction("localtime64_r", new Type[]{}, VOID);
    public static final InternalFunction localtime_r = new InternalFunction("localtime_r", new Type[]{}, VOID);
    public static final InternalFunction log = new InternalFunction("log", new Type[]{DOUBLE}, DOUBLE);
    public static final InternalFunction log10 = new InternalFunction("log10", new Type[]{}, VOID);
    public static final InternalFunction log10f = new InternalFunction("log10f", new Type[]{}, VOID);
    public static final InternalFunction log1p = new InternalFunction("log1p", new Type[]{}, VOID);
    public static final InternalFunction log1pf = new InternalFunction("log1pf", new Type[]{}, VOID);
    public static final InternalFunction logb = new InternalFunction("logb", new Type[]{}, VOID);
    public static final InternalFunction logbf = new InternalFunction("logbf", new Type[]{}, VOID);
    public static final InternalFunction logf = new InternalFunction("logf", new Type[]{}, VOID);
    public static final InternalFunction longjmp = new InternalFunction("longjmp", new Type[]{ADDRESS, ADDRESS}, VOID);
    public static final InternalFunction lrint = new InternalFunction("lrint", new Type[]{}, VOID);
    public static final InternalFunction lrintf = new InternalFunction("lrintf", new Type[]{}, VOID);
    public static final InternalFunction lround = new InternalFunction("lround", new Type[]{}, VOID);
    public static final InternalFunction lroundf = new InternalFunction("lroundf", new Type[]{}, VOID);
    public static final InternalFunction lseek = new InternalFunction("lseek", new Type[]{}, VOID);
    public static final InternalFunction lseek64 = new InternalFunction("lseek64", new Type[]{}, VOID);
    public static final InternalFunction lstat = new InternalFunction("lstat", new Type[]{}, VOID);
    public static final InternalFunction madvise = new InternalFunction("madvise", new Type[]{}, VOID);
    public static final InternalFunction mallinfo = new InternalFunction("mallinfo", new Type[]{}, VOID);
    public static final InternalFunction mbrtowc = new InternalFunction("mbrtowc", new Type[]{}, VOID);
    public static final InternalFunction mbsrtowcs = new InternalFunction("mbsrtowcs", new Type[]{}, VOID);
    public static final InternalFunction mbstowcs = new InternalFunction("mbstowcs", new Type[]{}, VOID);
    public static final InternalFunction memalign = new InternalFunction("memalign", new Type[]{}, VOID);
    public static final InternalFunction memccpy = new InternalFunction("memccpy", new Type[]{}, VOID);
    public static final InternalFunction memchr = new InternalFunction("memchr", new Type[]{}, VOID);
    public static final InternalFunction memcmp = new InternalFunction("memcmp", new Type[]{ADDRESS, ADDRESS, INT}, INT);
    public static final InternalFunction memmem = new InternalFunction("memmem", new Type[]{}, VOID);
    public static final InternalFunction memrchr = new InternalFunction("memrchr", new Type[]{}, VOID);
    public static final InternalFunction memswap = new InternalFunction("memswap", new Type[]{}, VOID);
    public static final InternalFunction mincore = new InternalFunction("mincore", new Type[]{}, VOID);
    public static final InternalFunction mkdir = new InternalFunction("mkdir", new Type[]{}, VOID);
    public static final InternalFunction mkdtemp = new InternalFunction("mkdtemp", new Type[]{}, VOID);
    public static final InternalFunction mknod = new InternalFunction("mknod", new Type[]{}, VOID);
    public static final InternalFunction mkstemp = new InternalFunction("mkstemp", new Type[]{}, VOID);
    public static final InternalFunction mktemp = new InternalFunction("mktemp", new Type[]{}, VOID);
    public static final InternalFunction mktime = new InternalFunction("mktime", new Type[]{ADDRESS}, VOID);
    public static final InternalFunction mktime64 = new InternalFunction("mktime64", new Type[]{}, VOID);
    public static final InternalFunction mlock = new InternalFunction("mlock", new Type[]{}, VOID);
    public static final InternalFunction mmap = new InternalFunction("mmap", new Type[]{ADDRESS, INT, INT, INT, INT, INT}, ADDRESS);
    public static final InternalFunction modf = new InternalFunction("modf", new Type[]{}, VOID);
    public static final InternalFunction modff = new InternalFunction("modff", new Type[]{}, VOID);
    public static final InternalFunction mprotect = new InternalFunction("mprotect", new Type[]{}, VOID);
    public static final InternalFunction mrand48 = new InternalFunction("mrand48", new Type[]{}, VOID);
    public static final InternalFunction seed48 = new InternalFunction("seed48", new Type[]{}, VOID);
    public static final InternalFunction mremap = new InternalFunction("mremap", new Type[]{}, VOID);
    public static final InternalFunction msync = new InternalFunction("msync", new Type[]{}, VOID);
    public static final InternalFunction munlock = new InternalFunction("munlock", new Type[]{}, VOID);
    public static final InternalFunction munmap = new InternalFunction("munmap", new Type[]{}, VOID);
    public static final InternalFunction nanosleep = new InternalFunction("nanosleep", new Type[]{}, VOID, Kind.SKIP);
    public static final InternalFunction nearbyint = new InternalFunction("nearbyint", new Type[]{}, VOID);
    public static final InternalFunction nextafter = new InternalFunction("nextafter", new Type[]{}, VOID);
    public static final InternalFunction nextafterf = new InternalFunction("nextafterf", new Type[]{}, VOID);
    public static final InternalFunction nice = new InternalFunction("nice", new Type[]{}, VOID);
    public static final InternalFunction nsdispatch = new InternalFunction("nsdispatch", new Type[]{}, VOID);
    public static final InternalFunction openat = new InternalFunction("openat", new Type[]{}, VOID);
    public static final InternalFunction opendir = new InternalFunction("opendir", new Type[]{}, VOID);
    public static final InternalFunction openlog = new InternalFunction("openlog", new Type[]{}, VOID);
    public static final InternalFunction pathconf = new InternalFunction("pathconf", new Type[]{}, VOID);
    public static final InternalFunction pause = new InternalFunction("pause", new Type[]{}, VOID);
    public static final InternalFunction pclose = new InternalFunction("pclose", new Type[]{}, VOID);
    public static final InternalFunction pipe = new InternalFunction("pipe", new Type[]{}, VOID);
    public static final InternalFunction pipe2 = new InternalFunction("pipe2", new Type[]{}, VOID);
    public static final InternalFunction poll = new InternalFunction("poll", new Type[]{}, VOID);
    public static final InternalFunction popen = new InternalFunction("popen", new Type[]{}, VOID);
    public static final InternalFunction pow = new InternalFunction("pow", new Type[]{DOUBLE, DOUBLE}, DOUBLE);
    public static final InternalFunction powf = new InternalFunction("powf", new Type[]{}, VOID);
    public static final InternalFunction prctl = new InternalFunction("prctl", new Type[]{}, VOID);
    public static final InternalFunction pread = new InternalFunction("pread", new Type[]{}, VOID);
    public static final InternalFunction property_get = new InternalFunction("property_get", new Type[]{}, VOID);
    public static final InternalFunction pselect = new InternalFunction("pselect", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_destroy = new InternalFunction("pthread_attr_destroy", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_getdetachstate = new InternalFunction("pthread_attr_getdetachstate", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_getguardsize = new InternalFunction("pthread_attr_getguardsize", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_getschedparam = new InternalFunction("pthread_attr_getschedparam", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_getschedpolicy = new InternalFunction("pthread_attr_getschedpolicy", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_getstack = new InternalFunction("pthread_attr_getstack", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_getstackaddr = new InternalFunction("pthread_attr_getstackaddr", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_getstacksize = new InternalFunction("pthread_attr_getstacksize", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_init = new InternalFunction("pthread_attr_init", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_setdetachstate = new InternalFunction("pthread_attr_setdetachstate", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_setguardsize = new InternalFunction("pthread_attr_setguardsize", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_setschedparam = new InternalFunction("pthread_attr_setschedparam", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_setschedpolicy = new InternalFunction("pthread_attr_setschedpolicy", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_setscope = new InternalFunction("pthread_attr_setscope", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_setstack = new InternalFunction("pthread_attr_setstack", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_setstackaddr = new InternalFunction("pthread_attr_setstackaddr", new Type[]{}, VOID);
    public static final InternalFunction pthread_attr_setstacksize = new InternalFunction("pthread_attr_setstacksize", new Type[]{}, VOID);
    public static final InternalFunction pthread_cond_broadcast = new InternalFunction("pthread_cond_broadcast", new Type[]{}, VOID);
    public static final InternalFunction pthread_cond_destroy = new InternalFunction("pthread_cond_destroy", new Type[]{}, VOID);
    public static final InternalFunction pthread_cond_init = new InternalFunction("pthread_cond_init", new Type[]{}, VOID);
    public static final InternalFunction pthread_cond_signal = new InternalFunction("pthread_cond_signal", new Type[]{}, VOID);
    public static final InternalFunction pthread_cond_timedwait = new InternalFunction("pthread_cond_timedwait", new Type[]{}, VOID);
    public static final InternalFunction pthread_cond_timedwait_monotonic = new InternalFunction("pthread_cond_timedwait_monotonic", new Type[]{}, VOID);
    public static final InternalFunction pthread_cond_timedwait_monotonic_np = new InternalFunction("pthread_cond_timedwait_monotonic_np", new Type[]{}, VOID);
    public static final InternalFunction pthread_cond_timedwait_relative_np = new InternalFunction("pthread_cond_timedwait_relative_np", new Type[]{}, VOID);
    public static final InternalFunction pthread_cond_timeout_np = new InternalFunction("pthread_cond_timeout_np", new Type[]{}, VOID);
    public static final InternalFunction pthread_cond_wait = new InternalFunction("pthread_cond_wait", new Type[]{}, VOID);
    public static final InternalFunction pthread_condattr_destroy = new InternalFunction("pthread_condattr_destroy", new Type[]{}, VOID);
    public static final InternalFunction pthread_condattr_init = new InternalFunction("pthread_condattr_init", new Type[]{}, VOID);
    public static final InternalFunction pthread_condattr_setpshared = new InternalFunction("pthread_condattr_setpshared", new Type[]{}, VOID);
    public static final InternalFunction pthread_create = new InternalFunction("pthread_create", new Type[]{VOID, VOID, VOID, VOID}, INT);
    public static final InternalFunction pthread_detach = new InternalFunction("pthread_detach", new Type[]{VOID}, INT);
    public static final InternalFunction pthread_equal = new InternalFunction("pthread_equal", new Type[]{}, VOID);
    public static final InternalFunction pthread_exit = new InternalFunction("pthread_exit", new Type[]{}, VOID);
    public static final InternalFunction pthread_getattr_np = new InternalFunction("pthread_getattr_np", new Type[]{}, VOID);
    public static final InternalFunction pthread_getschedparam = new InternalFunction("pthread_getschedparam", new Type[]{VOID}, VOID);
    public static final InternalFunction pthread_join = new InternalFunction("pthread_join", new Type[]{}, VOID);
    public static final InternalFunction pthread_key_create = new InternalFunction("pthread_key_create", new Type[]{VOID, VOID}, INT);
    public static final InternalFunction pthread_key_delete = new InternalFunction("pthread_key_delete", new Type[]{VOID}, INT);
    public static final InternalFunction pthread_kill = new InternalFunction("pthread_kill", new Type[]{}, VOID);
    public static final InternalFunction pthread_mutex_destroy = new InternalFunction("pthread_mutex_destroy", new Type[]{}, VOID);
    public static final InternalFunction pthread_mutex_init = new InternalFunction("pthread_mutex_init", new Type[]{}, VOID);
    public static final InternalFunction pthread_mutex_trylock = new InternalFunction("pthread_mutex_trylock", new Type[]{}, VOID);
    public static final InternalFunction pthread_mutexattr_destroy = new InternalFunction("pthread_mutexattr_destroy", new Type[]{}, VOID);
    public static final InternalFunction pthread_mutexattr_init = new InternalFunction("pthread_mutexattr_init", new Type[]{}, VOID);
    public static final InternalFunction pthread_mutexattr_setpshared = new InternalFunction("pthread_mutexattr_setpshared", new Type[]{}, VOID);
    public static final InternalFunction pthread_mutexattr_settype = new InternalFunction("pthread_mutexattr_settype", new Type[]{}, VOID);
    public static final InternalFunction pthread_rwlock_destroy = new InternalFunction("pthread_rwlock_destroy", new Type[]{}, VOID);
    public static final InternalFunction pthread_rwlock_init = new InternalFunction("pthread_rwlock_init", new Type[]{}, VOID);
    public static final InternalFunction pthread_rwlock_rdlock = new InternalFunction("pthread_rwlock_rdlock", new Type[]{}, VOID);
    public static final InternalFunction pthread_rwlock_unlock = new InternalFunction("pthread_rwlock_unlock", new Type[]{}, VOID);
    public static final InternalFunction pthread_rwlock_wrlock = new InternalFunction("pthread_rwlock_wrlock", new Type[]{}, VOID);
    public static final InternalFunction pthread_self = new InternalFunction("pthread_self", new Type[]{}, VOID);
    public static final InternalFunction pthread_setname_np = new InternalFunction("pthread_setname_np", new Type[]{}, VOID);
    public static final InternalFunction pthread_setschedparam = new InternalFunction("pthread_setschedparam", new Type[]{}, VOID);
    public static final InternalFunction pthread_setspecific = new InternalFunction("pthread_setspecific", new Type[]{VOID, VOID}, VOID);
    public static final InternalFunction pthread_sigmask = new InternalFunction("pthread_sigmask", new Type[]{}, VOID);
    public static final InternalFunction ptrace = new InternalFunction("ptrace", new Type[]{}, VOID);
    public static final InternalFunction putc = new InternalFunction("putc", new Type[]{}, VOID);
    public static final InternalFunction putchar = new InternalFunction("putchar", new Type[]{}, VOID);
    public static final InternalFunction putenv = new InternalFunction("putenv", new Type[]{}, VOID);
    public static final InternalFunction puts = new InternalFunction("puts", new Type[]{VOID}, VOID, Kind.SKIP);
    public static final InternalFunction putwc = new InternalFunction("putwc", new Type[]{}, VOID);
    public static final InternalFunction pwrite = new InternalFunction("pwrite", new Type[]{}, VOID);
    public static final InternalFunction qsort = new InternalFunction("qsort", new Type[]{}, VOID);
    public static final InternalFunction raise = new InternalFunction("raise", new Type[]{}, VOID);
    public static final InternalFunction read = new InternalFunction("read", new Type[]{FILE, ADDRESS, INT}, VOID);
    public static final InternalFunction readdir = new InternalFunction("readdir", new Type[]{}, VOID);
    public static final InternalFunction readdir_r = new InternalFunction("readdir_r", new Type[]{}, VOID);
    public static final InternalFunction readlink = new InternalFunction("readlink", new Type[]{}, VOID);
    public static final InternalFunction readv = new InternalFunction("readv", new Type[]{}, VOID);
    public static final InternalFunction realpath = new InternalFunction("realpath", new Type[]{}, VOID);
    public static final InternalFunction recv = new InternalFunction("recv", new Type[]{}, VOID);
    public static final InternalFunction recvfrom = new InternalFunction("recvfrom", new Type[]{}, VOID);
    public static final InternalFunction recvmsg = new InternalFunction("recvmsg", new Type[]{}, VOID);
    public static final InternalFunction regcomp = new InternalFunction("regcomp", new Type[]{}, VOID);
    public static final InternalFunction regerror = new InternalFunction("regerror", new Type[]{}, VOID);
    public static final InternalFunction regexec = new InternalFunction("regexec", new Type[]{}, VOID);
    public static final InternalFunction regfree = new InternalFunction("regfree", new Type[]{}, VOID);
    public static final InternalFunction remainder = new InternalFunction("remainder", new Type[]{}, VOID);
    public static final InternalFunction remainderf = new InternalFunction("remainderf", new Type[]{}, VOID);
    public static final InternalFunction remove = new InternalFunction("remove", new Type[]{}, VOID);
    public static final InternalFunction remquo = new InternalFunction("remquo", new Type[]{}, VOID);
    public static final InternalFunction remquof = new InternalFunction("remquof", new Type[]{}, VOID);
    public static final InternalFunction rename = new InternalFunction("rename", new Type[]{}, VOID);
    public static final InternalFunction rewind = new InternalFunction("rewind", new Type[]{}, VOID);
    public static final InternalFunction rint = new InternalFunction("rint", new Type[]{}, VOID);
    public static final InternalFunction rintf = new InternalFunction("rintf", new Type[]{}, VOID);
    public static final InternalFunction rmdir = new InternalFunction("rmdir", new Type[]{}, VOID);
    public static final InternalFunction round = new InternalFunction("round", new Type[]{DOUBLE}, DOUBLE);
    public static final InternalFunction roundf = new InternalFunction("roundf", new Type[]{}, VOID);
    public static final InternalFunction sbrk = new InternalFunction("sbrk", new Type[]{}, VOID);
    public static final InternalFunction scalbn = new InternalFunction("scalbn", new Type[]{}, VOID);
    public static final InternalFunction scandir = new InternalFunction("scandir", new Type[]{}, VOID);
    public static final InternalFunction scanf = new InternalFunction("scanf", new Type[]{}, VOID);
    public static final InternalFunction sched_get_priority_max = new InternalFunction("sched_get_priority_max", new Type[]{}, VOID);
    public static final InternalFunction sched_get_priority_min = new InternalFunction("sched_get_priority_min", new Type[]{}, VOID);
    public static final InternalFunction sched_getparam = new InternalFunction("sched_getparam", new Type[]{}, VOID);
    public static final InternalFunction sched_setscheduler = new InternalFunction("sched_setscheduler", new Type[]{}, VOID);
    public static final InternalFunction sched_yield = new InternalFunction("sched_yield", new Type[]{}, VOID);
    public static final InternalFunction select = new InternalFunction("select", new Type[]{}, VOID);
    public static final InternalFunction sem_close = new InternalFunction("sem_close", new Type[]{}, VOID);
    public static final InternalFunction sem_destroy = new InternalFunction("sem_destroy", new Type[]{}, VOID);
    public static final InternalFunction sem_getvalue = new InternalFunction("sem_getvalue", new Type[]{}, VOID);
    public static final InternalFunction sem_init = new InternalFunction("sem_init", new Type[]{}, VOID);
    public static final InternalFunction sem_open = new InternalFunction("sem_open", new Type[]{}, VOID);
    public static final InternalFunction sem_post = new InternalFunction("sem_post", new Type[]{}, VOID);
    public static final InternalFunction sem_timedwait = new InternalFunction("sem_timedwait", new Type[]{}, VOID);
    public static final InternalFunction sem_trywait = new InternalFunction("sem_trywait", new Type[]{}, VOID);
    public static final InternalFunction sem_unlink = new InternalFunction("sem_unlink", new Type[]{}, VOID);
    public static final InternalFunction sem_wait = new InternalFunction("sem_wait", new Type[]{}, VOID);
    public static final InternalFunction send = new InternalFunction("send", new Type[]{}, VOID);
    public static final InternalFunction sendfile = new InternalFunction("sendfile", new Type[]{}, VOID);
    public static final InternalFunction sendmsg = new InternalFunction("sendmsg", new Type[]{}, VOID);
    public static final InternalFunction sendto = new InternalFunction("sendto", new Type[]{}, VOID);
    public static final InternalFunction set_sched_policy = new InternalFunction("set_sched_policy", new Type[]{}, VOID);
    public static final InternalFunction setbuf = new InternalFunction("setbuf", new Type[]{}, VOID);
    public static final InternalFunction setenv = new InternalFunction("setenv", new Type[]{}, VOID);
    public static final InternalFunction setgid = new InternalFunction("setgid", new Type[]{}, VOID);
    public static final InternalFunction setitimer = new InternalFunction("setitimer", new Type[]{}, VOID);
    public static final InternalFunction setjmp = new InternalFunction("setjmp", new Type[]{ADDRESS}, VOID);
    public static final InternalFunction setpgid = new InternalFunction("setpgid", new Type[]{}, VOID);
    public static final InternalFunction setpriority = new InternalFunction("setpriority", new Type[]{}, VOID);
    public static final InternalFunction setresuid = new InternalFunction("setresuid", new Type[]{}, VOID);
    public static final InternalFunction setrlimit = new InternalFunction("setrlimit", new Type[]{}, VOID);
    public static final InternalFunction setsid = new InternalFunction("setsid", new Type[]{}, VOID);
    public static final InternalFunction setsockopt = new InternalFunction("setsockopt", new Type[]{}, VOID);
    public static final InternalFunction settimeofday = new InternalFunction("settimeofday", new Type[]{}, VOID);
    public static final InternalFunction setvbuf = new InternalFunction("setvbuf", new Type[]{}, VOID);
    public static final InternalFunction shutdown = new InternalFunction("shutdown", new Type[]{}, VOID);
    public static final InternalFunction sigaltstack = new InternalFunction("sigaltstack", new Type[]{}, VOID);
    public static final InternalFunction siglongjmp = new InternalFunction("siglongjmp", new Type[]{}, VOID);
    public static final InternalFunction sigprocmask = new InternalFunction("sigprocmask", new Type[]{}, VOID);
    public static final InternalFunction sigsetjmp = new InternalFunction("sigsetjmp", new Type[]{}, VOID);
    public static final InternalFunction sigsuspend = new InternalFunction("sigsuspend", new Type[]{}, VOID);
    public static final InternalFunction sin = new InternalFunction("sin", new Type[]{}, VOID);
    public static final InternalFunction sinf = new InternalFunction("sinf", new Type[]{FLOAT}, VOID);
    public static final InternalFunction sinh = new InternalFunction("sinh", new Type[]{}, VOID);
    public static final InternalFunction sinhf = new InternalFunction("sinhf", new Type[]{}, VOID);
    public static final InternalFunction sk_free = new InternalFunction("sk_free", new Type[]{}, VOID);
    public static final InternalFunction sk_pop = new InternalFunction("sk_pop", new Type[]{}, VOID);
    public static final InternalFunction slCreateEngine = new InternalFunction("slCreateEngine", new Type[]{}, VOID);
    public static final InternalFunction sleep = new InternalFunction("sleep", new Type[]{INT}, VOID);
    public static final InternalFunction socket = new InternalFunction("socket", new Type[]{}, VOID);
    public static final InternalFunction socketpair = new InternalFunction("socketpair", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_bind_blob = new InternalFunction("sqlite3_bind_blob", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_bind_int = new InternalFunction("sqlite3_bind_int", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_bind_text = new InternalFunction("sqlite3_bind_text", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_close = new InternalFunction("sqlite3_close", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_column_blob = new InternalFunction("sqlite3_column_blob", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_column_bytes = new InternalFunction("sqlite3_column_bytes", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_column_int = new InternalFunction("sqlite3_column_int", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_column_text = new InternalFunction("sqlite3_column_text", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_errmsg = new InternalFunction("sqlite3_errmsg", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_exec = new InternalFunction("sqlite3_exec", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_finalize = new InternalFunction("sqlite3_finalize", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_open = new InternalFunction("sqlite3_open", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_prepare = new InternalFunction("sqlite3_prepare", new Type[]{}, VOID);
    public static final InternalFunction sqlite3_step = new InternalFunction("sqlite3_step", new Type[]{}, VOID);
    public static final InternalFunction sqrt = new InternalFunction("sqrt", new Type[]{}, VOID);
    public static final InternalFunction sqrtf = new InternalFunction("sqrtf", new Type[]{}, VOID);
    public static final InternalFunction srand48 = new InternalFunction("srand48", new Type[]{}, VOID);
    public static final InternalFunction sscanf = new InternalFunction("sscanf", new Type[]{}, VOID);
    public static final InternalFunction stat = new InternalFunction("stat", new Type[]{}, VOID);
    public static final InternalFunction statfs = new InternalFunction("statfs", new Type[]{}, VOID);
    public static final InternalFunction strcasestr = new InternalFunction("strcasestr", new Type[]{}, VOID);
    public static final InternalFunction strcat = new InternalFunction("strcat", new Type[]{STRING, STRING}, ADDRESS);
    public static final InternalFunction strchr = new InternalFunction("strchr", new Type[]{STRING, INT}, VOID);
    public static final InternalFunction strcoll = new InternalFunction("strcoll", new Type[]{}, VOID);
    public static final InternalFunction strcpy16 = new InternalFunction("strcpy16", new Type[]{}, VOID);
    public static final InternalFunction strcspn = new InternalFunction("strcspn", new Type[]{}, VOID);
    public static final InternalFunction strerror = new InternalFunction("strerror", new Type[]{}, VOID);
    public static final InternalFunction strftime = new InternalFunction("strftime", new Type[]{}, VOID);
    public static final InternalFunction strlcat = new InternalFunction("strlcat", new Type[]{}, VOID);
    public static final InternalFunction strlcpy = new InternalFunction("strlcpy", new Type[]{}, VOID);
    public static final InternalFunction strlen16 = new InternalFunction("strlen16", new Type[]{STRING}, VOID);
    public static final InternalFunction strlen32 = new InternalFunction("strlen32", new Type[]{STRING}, VOID);
    public static final InternalFunction strncasecmp = new InternalFunction("strncasecmp", new Type[]{}, VOID);
    public static final InternalFunction strncat = new InternalFunction("strncat", new Type[]{STRING, STRING, INT}, ADDRESS);
    public static final InternalFunction strncmp = new InternalFunction("strncmp", new Type[]{STRING, STRING, INT}, VOID);
    public static final InternalFunction strncmp16 = new InternalFunction("strncmp16", new Type[]{STRING, STRING, INT}, VOID);
    public static final InternalFunction strncpy = new InternalFunction("strncpy", new Type[]{STRING, STRING, INT}, ADDRESS);
    public static final InternalFunction strndup = new InternalFunction("strndup", new Type[]{}, VOID);
    public static final InternalFunction strnlen = new InternalFunction("strnlen", new Type[]{}, VOID);
    public static final InternalFunction strpbrk = new InternalFunction("strpbrk", new Type[]{}, VOID);
    public static final InternalFunction strptime = new InternalFunction("strptime", new Type[]{}, VOID);
    public static final InternalFunction strrchr = new InternalFunction("strrchr", new Type[]{}, VOID);
    public static final InternalFunction strsep = new InternalFunction("strsep", new Type[]{}, VOID);
    public static final InternalFunction strsignal = new InternalFunction("strsignal", new Type[]{}, VOID);
    public static final InternalFunction strspn = new InternalFunction("strspn", new Type[]{}, VOID);
    public static final InternalFunction strtod = new InternalFunction("strtod", new Type[]{}, VOID);
    public static final InternalFunction strtoimax = new InternalFunction("strtoimax", new Type[]{}, VOID);
    public static final InternalFunction strtok = new InternalFunction("strtok", new Type[]{}, VOID);
    public static final InternalFunction strtok_r = new InternalFunction("strtok_r", new Type[]{}, VOID);
    public static final InternalFunction strtol = new InternalFunction("strtol", new Type[]{}, VOID);
    public static final InternalFunction strtoll = new InternalFunction("strtoll", new Type[]{}, VOID);
    public static final InternalFunction strtoul = new InternalFunction("strtoul", new Type[]{}, VOID);
    public static final InternalFunction strtoull = new InternalFunction("strtoull", new Type[]{}, VOID);
    public static final InternalFunction strtoumax = new InternalFunction("strtoumax", new Type[]{}, VOID);
    public static final InternalFunction strxfrm = new InternalFunction("strxfrm", new Type[]{}, VOID);
    public static final InternalFunction strzcmp16 = new InternalFunction("strzcmp16", new Type[]{}, VOID);
    public static final InternalFunction swprintf = new InternalFunction("swprintf", new Type[]{}, VOID);
    public static final InternalFunction swscanf = new InternalFunction("swscanf", new Type[]{}, VOID);
    public static final InternalFunction symlink = new InternalFunction("symlink", new Type[]{}, VOID);
    public static final InternalFunction sync = new InternalFunction("sync", new Type[]{}, VOID);
    public static final InternalFunction syscall = new InternalFunction("syscall", new Type[]{}, VOID);
    public static final InternalFunction sysconf = new InternalFunction("sysconf", new Type[]{}, VOID);
    public static final InternalFunction syslog = new InternalFunction("syslog", new Type[]{}, VOID);
    public static final InternalFunction system = new InternalFunction("system", new Type[]{}, VOID);
    public static final InternalFunction systemTime = new InternalFunction("systemTime", new Type[]{}, VOID);
    public static final InternalFunction tan = new InternalFunction("tan", new Type[]{}, VOID);
    public static final InternalFunction tanf = new InternalFunction("tanf", new Type[]{}, VOID);
    public static final InternalFunction tanh = new InternalFunction("tanh", new Type[]{}, VOID);
    public static final InternalFunction tanhf = new InternalFunction("tanhf", new Type[]{}, VOID);
    public static final InternalFunction tgamma = new InternalFunction("tgamma", new Type[]{}, VOID);
    public static final InternalFunction time = new InternalFunction("time", new Type[]{ADDRESS}, VOID);
    public static final InternalFunction timegm64 = new InternalFunction("timegm64", new Type[]{}, VOID);
    public static final InternalFunction timer_create = new InternalFunction("timer_create", new Type[]{}, VOID);
    public static final InternalFunction timer_delete = new InternalFunction("timer_delete", new Type[]{}, VOID);
    public static final InternalFunction timer_settime = new InternalFunction("timer_settime", new Type[]{}, VOID);
    public static final InternalFunction times = new InternalFunction("times", new Type[]{}, VOID);
    public static final InternalFunction tkill = new InternalFunction("tkill", new Type[]{}, VOID);
    public static final InternalFunction tmpfile = new InternalFunction("tmpfile", new Type[]{}, VOID);
    public static final InternalFunction tmpnam = new InternalFunction("tmpnam", new Type[]{}, VOID);
    public static final InternalFunction tolower = new InternalFunction("tolower", new Type[]{}, VOID);
    public static final InternalFunction toupper = new InternalFunction("toupper", new Type[]{}, VOID);
    public static final InternalFunction towlower = new InternalFunction("towlower", new Type[]{}, VOID);
    public static final InternalFunction towupper = new InternalFunction("towupper", new Type[]{}, VOID);
    public static final InternalFunction trunc = new InternalFunction("trunc", new Type[]{}, VOID);
    public static final InternalFunction truncate = new InternalFunction("truncate", new Type[]{}, VOID);
    public static final InternalFunction truncf = new InternalFunction("truncf", new Type[]{}, VOID);
    public static final InternalFunction tzset = new InternalFunction("tzset", new Type[]{}, VOID);
    public static final InternalFunction umask = new InternalFunction("umask", new Type[]{}, VOID);
    public static final InternalFunction uname = new InternalFunction("uname", new Type[]{}, VOID);
    public static final InternalFunction uncompress = new InternalFunction("uncompress", new Type[]{}, VOID);
    public static final InternalFunction ungetc = new InternalFunction("ungetc", new Type[]{}, VOID);
    public static final InternalFunction ungetwc = new InternalFunction("ungetwc", new Type[]{}, VOID);
    public static final InternalFunction unlink = new InternalFunction("unlink", new Type[]{}, VOID);
    public static final InternalFunction unsetenv = new InternalFunction("unsetenv", new Type[]{}, VOID);
    public static final InternalFunction usleep = new InternalFunction("usleep", new Type[]{}, VOID, Kind.SKIP);
    public static final InternalFunction utf16_to_utf8 = new InternalFunction("utf16_to_utf8", new Type[]{}, VOID);
    public static final InternalFunction utf16_to_utf8_length = new InternalFunction("utf16_to_utf8_length", new Type[]{}, VOID);
    public static final InternalFunction utf32_from_utf8_at = new InternalFunction("utf32_from_utf8_at", new Type[]{}, VOID);
    public static final InternalFunction utf32_to_utf8 = new InternalFunction("utf32_to_utf8", new Type[]{}, VOID);
    public static final InternalFunction utf32_to_utf8_length = new InternalFunction("utf32_to_utf8_length", new Type[]{}, VOID);
    public static final InternalFunction utf8_to_utf16 = new InternalFunction("utf8_to_utf16", new Type[]{}, VOID);
    public static final InternalFunction utf8_to_utf16_length = new InternalFunction("utf8_to_utf16_length", new Type[]{}, VOID);
    public static final InternalFunction utf8_to_utf32 = new InternalFunction("utf8_to_utf32", new Type[]{}, VOID);
    public static final InternalFunction utf8_to_utf32_length = new InternalFunction("utf8_to_utf32_length", new Type[]{}, VOID);
    public static final InternalFunction utime = new InternalFunction("utime", new Type[]{}, VOID);
    public static final InternalFunction utimes = new InternalFunction("utimes", new Type[]{}, VOID);
    public static final InternalFunction valloc = new InternalFunction("valloc", new Type[]{}, VOID);
    public static final InternalFunction vasprintf = new InternalFunction("vasprintf", new Type[]{}, VOID);
    public static final InternalFunction vfprintf = new InternalFunction("vfprintf", new Type[]{}, VOID);
    public static final InternalFunction vfscanf = new InternalFunction("vfscanf", new Type[]{}, VOID);
    public static final InternalFunction vprintf = new InternalFunction("vprintf", new Type[]{}, VOID);
    public static final InternalFunction vsnprintf = new InternalFunction("vsnprintf", new Type[]{ADDRESS, INT, FORMAT, VA_LIST}, VOID);
    public static final InternalFunction vsprintf = new InternalFunction("vsprintf", new Type[]{}, VOID);
    public static final InternalFunction vsscanf = new InternalFunction("vsscanf", new Type[]{}, VOID);
    public static final InternalFunction vswprintf = new InternalFunction("vswprintf", new Type[]{}, VOID);
    public static final InternalFunction vsyslog = new InternalFunction("vsyslog", new Type[]{}, VOID);
    public static final InternalFunction vwprintf = new InternalFunction("vwprintf", new Type[]{}, VOID);
    public static final InternalFunction wait = new InternalFunction("wait", new Type[]{INT}, VOID);
    public static final InternalFunction waitid = new InternalFunction("waitid", new Type[]{}, VOID);
    public static final InternalFunction waitpid = new InternalFunction("waitpid", new Type[]{}, VOID);
    public static final InternalFunction wcrtomb = new InternalFunction("wcrtomb", new Type[]{}, VOID);
    public static final InternalFunction wcscat = new InternalFunction("wcscat", new Type[]{}, VOID);
    public static final InternalFunction wcschr = new InternalFunction("wcschr", new Type[]{}, VOID);
    public static final InternalFunction wcscmp = new InternalFunction("wcscmp", new Type[]{}, VOID);
    public static final InternalFunction wcscoll = new InternalFunction("wcscoll", new Type[]{}, VOID);
    public static final InternalFunction wcscpy = new InternalFunction("wcscpy", new Type[]{}, VOID);
    public static final InternalFunction wcsftime = new InternalFunction("wcsftime", new Type[]{}, VOID);
    public static final InternalFunction wcslen = new InternalFunction("wcslen", new Type[]{}, VOID);
    public static final InternalFunction wcsncat = new InternalFunction("wcsncat", new Type[]{}, VOID);
    public static final InternalFunction wcsncmp = new InternalFunction("wcsncmp", new Type[]{}, VOID);
    public static final InternalFunction wcsncpy = new InternalFunction("wcsncpy", new Type[]{}, VOID);
    public static final InternalFunction wcsrtombs = new InternalFunction("wcsrtombs", new Type[]{}, VOID);
    public static final InternalFunction wcsspn = new InternalFunction("wcsspn", new Type[]{}, VOID);
    public static final InternalFunction wcsstr = new InternalFunction("wcsstr", new Type[]{}, VOID);
    public static final InternalFunction wcstol = new InternalFunction("wcstol", new Type[]{}, VOID);
    public static final InternalFunction wcstombs = new InternalFunction("wcstombs", new Type[]{}, VOID);
    public static final InternalFunction wcsxfrm = new InternalFunction("wcsxfrm", new Type[]{}, VOID);
    public static final InternalFunction wctob = new InternalFunction("wctob", new Type[]{}, VOID);
    public static final InternalFunction wctype = new InternalFunction("wctype", new Type[]{STRING}, INT);
    public static final InternalFunction wmemchr = new InternalFunction("wmemchr", new Type[]{}, VOID);
    public static final InternalFunction wmemcmp = new InternalFunction("wmemcmp", new Type[]{}, VOID);
    public static final InternalFunction wmemcpy = new InternalFunction("wmemcpy", new Type[]{}, VOID);
    public static final InternalFunction wmemmove = new InternalFunction("wmemmove", new Type[]{}, VOID);
    public static final InternalFunction wmemset = new InternalFunction("wmemset", new Type[]{}, VOID);
    public static final InternalFunction write = new InternalFunction("write", new Type[]{}, VOID);
    public static final InternalFunction writev = new InternalFunction("writev", new Type[]{}, VOID);
    public static final InternalFunction wscanf = new InternalFunction("wscanf", new Type[]{}, VOID);
    public static final InternalFunction xaCreateEngine = new InternalFunction("xaCreateEngine", new Type[]{}, VOID);
    public static final InternalFunction zError = new InternalFunction("zError", new Type[]{}, VOID);
    public static final InternalFunction zlibCompileFlags = new InternalFunction("zlibCompileFlags", new Type[]{}, VOID);
    //    public static final Type FREE = RefType.v("FREE");
    private static final Logger logger = LoggerFactory.getLogger(InternalFunction.class);
    public static Map<String, AbstractValue> functionMap = new HashMap<String, AbstractValue>();
    private final Kind kind;

    static {
        addFunction(hw_get_module);
        addFunction(calloc);
        addFunction(malloc);
        addFunction(realloc);
        addFunction(_Znwj);
        addFunction(_Znaj);
        addFunction(free);
        addFunction(_ZdaPv);
        addFunction(_ZdlPv);
        addFunction(printf);
        addFunction(snprintf);
        addFunction(asprintf);
        addFunction(sprintf);
        addFunction(memcpy);
        addFunction(__aeabi_memcpy);
        addFunction(__aeabi_memset);
        addFunction(__aeabi_d2uiz);
        addFunction(__aeabi_ul2d);
        addFunction(setlocale);
        addFunction(memset);
        addFunction(pthread_getspecific);
        addFunction(getpid);
        addFunction(strlen);
        addFunction(strdup);
        addFunction(strstr);
        addFunction(strcpy);
        addFunction(strcmp);
        addFunction(strcasecmp);
        addFunction(memmove);
        addFunction(pthread_mutex_lock);
        addFunction(pthread_mutex_unlock);
        addFunction(pthread_once);
        addFunction(lrand48);
        addFunction(sigaction);
        addFunction(__errno);
        addFunction(strerror_r);
        addFunction(perror);
        addFunction(ioctl);
        addFunction(close);
        addFunction(open);
        addFunction(fopen);
        addFunction(fclose);
        addFunction(setuid);
        addFunction(fwrite);
        addFunction(dlopen);
        addFunction(dlsym);
        addFunction(dlclose);
        addFunction(abort);
        addFunction(__stack_chk_fail);
        addFunction(AAssetDir_close);
        addFunction(AAssetDir_getNextFileName);
        addFunction(AAssetManager_fromJava);
        addFunction(AAssetManager_open);
        addFunction(AAssetManager_openDir);
        addFunction(AAsset_close);
        addFunction(AAsset_getBuffer);
        addFunction(AAsset_getLength);
        addFunction(AAsset_getRemainingLength);
        addFunction(AAsset_openFileDescriptor);
        addFunction(AAsset_read);
        addFunction(AAsset_seek);
        addFunction(AConfiguration_delete);
        addFunction(AConfiguration_fromAssetManager);
        addFunction(AConfiguration_getCountry);
        addFunction(AConfiguration_getLanguage);
        addFunction(AConfiguration_new);
        addFunction(AES_cbc_encrypt);
        addFunction(AES_decrypt);
        addFunction(AES_encrypt);
        addFunction(AES_set_decrypt_key);
        addFunction(AES_set_encrypt_key);
        addFunction(AInputEvent_getDeviceId);
        addFunction(AInputEvent_getSource);
        addFunction(AInputEvent_getType);
        addFunction(AInputQueue_attachLooper);
        addFunction(AInputQueue_detachLooper);
        addFunction(AInputQueue_finishEvent);
        addFunction(AInputQueue_getEvent);
        addFunction(AInputQueue_preDispatchEvent);
        addFunction(AKeyEvent_getAction);
        addFunction(AKeyEvent_getDownTime);
        addFunction(AKeyEvent_getEventTime);
        addFunction(AKeyEvent_getFlags);
        addFunction(AKeyEvent_getKeyCode);
        addFunction(AKeyEvent_getMetaState);
        addFunction(AKeyEvent_getRepeatCount);
        addFunction(AKeyEvent_getScanCode);
        addFunction(ALooper_acquire);
        addFunction(ALooper_addFd);
        addFunction(ALooper_forThread);
        addFunction(ALooper_pollAll);
        addFunction(ALooper_prepare);
        addFunction(ALooper_release);
        addFunction(ALooper_removeFd);
        addFunction(AMotionEvent_getAction);
        addFunction(AMotionEvent_getDownTime);
        addFunction(AMotionEvent_getEdgeFlags);
        addFunction(AMotionEvent_getEventTime);
        addFunction(AMotionEvent_getFlags);
        addFunction(AMotionEvent_getHistoricalEventTime);
        addFunction(AMotionEvent_getHistoricalPressure);
        addFunction(AMotionEvent_getHistoricalSize);
        addFunction(AMotionEvent_getHistoricalX);
        addFunction(AMotionEvent_getHistoricalY);
        addFunction(AMotionEvent_getHistorySize);
        addFunction(AMotionEvent_getMetaState);
        addFunction(AMotionEvent_getOrientation);
        addFunction(AMotionEvent_getPointerCount);
        addFunction(AMotionEvent_getPointerId);
        addFunction(AMotionEvent_getPressure);
        addFunction(AMotionEvent_getSize);
        addFunction(AMotionEvent_getToolMajor);
        addFunction(AMotionEvent_getToolMinor);
        addFunction(AMotionEvent_getTouchMajor);
        addFunction(AMotionEvent_getTouchMinor);
        addFunction(AMotionEvent_getX);
        addFunction(AMotionEvent_getXPrecision);
        addFunction(AMotionEvent_getY);
        addFunction(AMotionEvent_getYPrecision);
        addFunction(ANativeActivity_finish);
        addFunction(ANativeWindow_acquire);
        addFunction(ANativeWindow_fromSurface);
        addFunction(ANativeWindow_getFormat);
        addFunction(ANativeWindow_getHeight);
        addFunction(ANativeWindow_getWidth);
        addFunction(ANativeWindow_lock);
        addFunction(ANativeWindow_release);
        addFunction(ANativeWindow_setBuffersGeometry);
        addFunction(ANativeWindow_unlockAndPost);
        addFunction(ASN1_INTEGER_get);
        addFunction(ASensorEventQueue_disableSensor);
        addFunction(ASensorEventQueue_enableSensor);
        addFunction(ASensorEventQueue_getEvents);
        addFunction(ASensorEventQueue_hasEvents);
        addFunction(ASensorEventQueue_setEventRate);
        addFunction(ASensorManager_createEventQueue);
        addFunction(ASensorManager_destroyEventQueue);
        addFunction(ASensorManager_getDefaultSensor);
        addFunction(ASensorManager_getInstance);
        addFunction(ASensorManager_getSensorList);
        addFunction(ASensor_getMinDelay);
        addFunction(ASensor_getName);
        addFunction(ASensor_getResolution);
        addFunction(ASensor_getType);
        addFunction(ASensor_getVendor);
        addFunction(AndroidBitmap_getInfo);
        addFunction(AndroidBitmap_lockPixels);
        addFunction(AndroidBitmap_unlockPixels);
        addFunction(BIO_ctrl);
        addFunction(BIO_f_base64);
        addFunction(BIO_free);
        addFunction(BIO_free_all);
        addFunction(BIO_gets);
        addFunction(BIO_new);
        addFunction(BIO_new_file);
        addFunction(BIO_new_mem_buf);
        addFunction(BIO_new_socket);
        addFunction(BIO_new_ssl_connect);
        addFunction(BIO_push);
        addFunction(BIO_read);
        addFunction(BIO_s_mem);
        addFunction(BIO_set_flags);
        addFunction(BIO_write);
        addFunction(BN_CTX_free);
        addFunction(BN_CTX_new);
        addFunction(BN_bin2bn);
        addFunction(BN_bn2bin);
        addFunction(BN_bn2hex);
        addFunction(BN_cmp);
        addFunction(BN_copy);
        addFunction(BN_free);
        addFunction(BN_hex2bn);
        addFunction(BN_mod_exp);
        addFunction(BN_new);
        addFunction(BN_num_bits);
        addFunction(BN_set_word);
        addFunction(BN_sub_word);
        addFunction(BN_value_one);
        addFunction(CRYPTO_cleanup_all_ex_data);
        addFunction(CRYPTO_free);
        addFunction(CRYPTO_get_locking_callback);
        addFunction(CRYPTO_num_locks);
        addFunction(CRYPTO_set_locking_callback);
        addFunction(DES_ecb3_encrypt);
        addFunction(DES_set_key);
        addFunction(DH_compute_key);
        addFunction(DH_free);
        addFunction(DH_generate_key);
        addFunction(DH_new);
        addFunction(ERR_error_string);
        addFunction(ERR_error_string_n);
        addFunction(ERR_free_strings);
        addFunction(ERR_get_error);
        addFunction(ERR_peek_last_error);
        addFunction(ERR_print_errors_fp);
        addFunction(ERR_remove_state);
        addFunction(EVP_aes_256_cbc);
        addFunction(EVP_BytesToKey);
        addFunction(EVP_CIPHER_CTX_cleanup);
        addFunction(EVP_CIPHER_CTX_init);
        addFunction(EVP_CIPHER_CTX_set_padding);
        addFunction(EVP_CIPHER_block_size);
        addFunction(EVP_CIPHER_iv_length);
        addFunction(EVP_CIPHER_key_length);
        addFunction(EVP_CIPHER_nid);
        addFunction(EVP_CipherFinal);
        addFunction(EVP_CipherInit);
        addFunction(EVP_CipherUpdate);
        addFunction(EVP_DecryptFinal_ex);
        addFunction(EVP_DecryptInit);
        addFunction(EVP_DecryptInit_ex);
        addFunction(EVP_DecryptUpdate);
        addFunction(EVP_EncryptFinal);
        addFunction(EVP_EncryptFinal_ex);
        addFunction(EVP_EncryptInit);
        addFunction(EVP_EncryptInit_ex);
        addFunction(EVP_EncryptUpdate);
        addFunction(EVP_MD_size);
        addFunction(EVP_PKEY_bits);
        addFunction(EVP_PKEY_free);
        addFunction(EVP_PKEY_get1_RSA);
        addFunction(EVP_PKEY_size);
        addFunction(EVP_aes_128_ecb);
        addFunction(EVP_cleanup);
        addFunction(EVP_des_cbc);
        addFunction(EVP_des_ede3_cbc);
        addFunction(EVP_get_cipherbyname);
        addFunction(EVP_rc4);
        addFunction(EVP_sha1);
        addFunction(HMAC_CTX_cleanup);
        addFunction(HMAC_CTX_init);
        addFunction(HMAC_Final);
        addFunction(HMAC_Init_ex);
        addFunction(HMAC_Update);
        addFunction(MD5_Final);
        addFunction(MD5_Init);
        addFunction(MD5_Update);
        addFunction(OBJ_nid2sn);
        addFunction(OPENSSL_add_all_algorithms_noconf);
        addFunction(PKCS5_PBKDF2_HMAC_SHA1);
        addFunction(PKCS7_free);
        addFunction(RAND_add);
        addFunction(RAND_bytes);
        addFunction(RSA_free);
        addFunction(RSA_generate_key_ex);
        addFunction(RSA_new);
        addFunction(RSA_private_decrypt);
        addFunction(RSA_public_encrypt);
        addFunction(RSA_sign);
        addFunction(RSA_size);
        addFunction(RSA_verify);
        addFunction(SHA1);
        addFunction(SHA1Final);
        addFunction(SHA1Init);
        addFunction(SHA1Update);
        addFunction(SHA1_Final);
        addFunction(SHA1_Init);
        addFunction(SHA1_Update);
        addFunction(SSL_COMP_get_compression_methods);
        addFunction(SSL_CTX_free);
        addFunction(SSL_CTX_load_verify_locations);
        addFunction(SSL_CTX_new);
        addFunction(SSL_CTX_set_verify);
        addFunction(SSL_CTX_use_PrivateKey_file);
        addFunction(SSL_CTX_use_certificate_chain_file);
        addFunction(SSL_accept);
        addFunction(SSL_connect);
        addFunction(SSL_ctrl);
        addFunction(SSL_free);
        addFunction(SSL_get_error);
        addFunction(SSL_get_peer_certificate);
        addFunction(SSL_get_shutdown);
        addFunction(SSL_get_verify_result);
        addFunction(SSL_library_init);
        addFunction(SSL_load_error_strings);
        addFunction(SSL_new);
        addFunction(SSL_pending);
        addFunction(SSL_read);
        addFunction(SSL_set_bio);
        addFunction(SSL_set_fd);
        addFunction(SSL_shutdown);
        addFunction(SSL_write);
        addFunction(SSLv23_client_method);
        addFunction(TLSv1_client_method);
        addFunction(TLSv1_server_method);
        addFunction(X509_NAME_free);
        addFunction(X509_NAME_get_text_by_NID);
        addFunction(X509_NAME_oneline);
        addFunction(X509_NAME_print_ex);
        addFunction(X509_free);
        addFunction(X509_get_issuer_name);
        addFunction(X509_get_pubkey);
        addFunction(X509_get_serialNumber);
        addFunction(X509_get_subject_name);
        addFunction(X509_new);
        addFunction(XML_ErrorString);
        addFunction(XML_GetBuffer);
        addFunction(XML_GetCurrentLineNumber);
        addFunction(XML_GetErrorCode);
        addFunction(XML_ParseBuffer);
        addFunction(XML_ParserCreate);
        addFunction(XML_ParserFree);
        addFunction(XML_SetCharacterDataHandler);
        addFunction(XML_SetDoctypeDeclHandler);
        addFunction(XML_SetElementHandler);
        addFunction(XML_SetUserData);
        addFunction(_ZN11GraphicsJNI15getNativeBitmapEP7_JNIEnvP8_jobject);
        addFunction(_ZN7SkPaint12setAntiAliasEb);
        addFunction(_ZN7SkPaint15setFilterBitmapEb);
        addFunction(_ZN7SkPaint9setDitherEb);
        addFunction(_ZN7SkPaintC1Ev);
        addFunction(_ZN7SkPaintD1Ev);
        addFunction(_ZN7android10AudioTrack11getPositionEPj);
        addFunction(_ZN7android10AudioTrack4stopEv);
        addFunction(_ZN7android10AudioTrack5flushEv);
        addFunction(_ZN7android10AudioTrack5pauseEv);
        addFunction(_ZN7android10AudioTrack5startEv);
        addFunction(_ZN7android10AudioTrack5writeEPKvj);
        addFunction(_ZN7android10AudioTrack9setVolumeEff);
        addFunction(_ZN7android10AudioTrackC1EijiiijPFviPvS1_ES1_i);
        addFunction(_ZN7android10AudioTrackC1EijiiijPFviPvS1_ES1_ii);
        addFunction(_ZN7android10AudioTrackD1Ev);
        addFunction(_ZN7android10IInterface8asBinderEv);
        addFunction(_ZN7android10IInterfaceD0Ev);
        addFunction(_ZN7android10IInterfaceD1Ev);
        addFunction(_ZN7android10VectorImpl13finish_vectorEv);
        addFunction(_ZN7android10VectorImpl13removeItemsAtEjj);
        addFunction(_ZN7android10VectorImpl16editItemLocationEj);
        addFunction(_ZN7android10VectorImpl19reservedVectorImpl1Ev);
        addFunction(_ZN7android10VectorImpl19reservedVectorImpl2Ev);
        addFunction(_ZN7android10VectorImpl19reservedVectorImpl3Ev);
        addFunction(_ZN7android10VectorImpl19reservedVectorImpl4Ev);
        addFunction(_ZN7android10VectorImpl19reservedVectorImpl5Ev);
        addFunction(_ZN7android10VectorImpl19reservedVectorImpl6Ev);
        addFunction(_ZN7android10VectorImpl19reservedVectorImpl7Ev);
        addFunction(_ZN7android10VectorImpl19reservedVectorImpl8Ev);
        addFunction(_ZN7android10VectorImpl3addEPKv);
        addFunction(_ZN7android10VectorImpl4pushEPKv);
        addFunction(_ZN7android10VectorImpl4pushEv);
        addFunction(_ZN7android10VectorImpl4sortEPFiPKvS2_E);
        addFunction(_ZN7android10VectorImpl5clearEv);
        addFunction(_ZN7android10VectorImpl8insertAtEPKvjj);
        addFunction(_ZN7android10VectorImplC2ERKS0_);
        addFunction(_ZN7android10VectorImplC2Ejj);
        addFunction(_ZN7android10VectorImplD2Ev);
        addFunction(_ZN7android11AudioPlayer19getMediaTimeMappingEPxS1_);
        addFunction(_ZN7android11AudioPlayer5pauseEb);
        addFunction(_ZN7android11AudioPlayer5pauseEv);
        addFunction(_ZN7android11AudioPlayer5startEb);
        addFunction(_ZN7android11AudioPlayer6resumeEv);
        addFunction(_ZN7android11AudioPlayer9setSourceERKNS_2spINS_11MediaSourceEEE);
        addFunction(_ZN7android11AudioPlayerC1ERKNS_2spINS_15MediaPlayerBase9AudioSinkEEE);
        addFunction(_ZN7android11AudioPlayerC1ERKNS_2spINS_15MediaPlayerBase9AudioSinkEEEPNS_13AwesomePlayerE);
        addFunction(_ZN7android11AudioSystem16getOutputLatencyEPji);
        addFunction(_ZN7android11AudioSystem16getOutputLatencyEPj19audio_stream_type_t);
        addFunction(_ZN7android11AudioSystem17newAudioSessionIdEv);
        addFunction(_ZN7android11AudioSystem19getOutputFrameCountEPii);
        addFunction(_ZN7android11AudioSystem21getOutputSamplingRateEPii);
        addFunction(_ZN7android11MediaBuffer11setObserverEPNS_19MediaBufferObserverE);
        addFunction(_ZN7android11MediaBuffer5cloneEv);
        addFunction(_ZN7android11MediaBuffer7add_refEv);
        addFunction(_ZN7android11MediaBuffer7releaseEv);
        addFunction(_ZN7android11MediaBuffer9meta_dataEv);
        addFunction(_ZN7android11MediaBuffer9set_rangeEjj);
        addFunction(_ZN7android11MediaBufferC1EPvj);
        addFunction(_ZN7android11MediaBufferC1Ej);
        addFunction(_ZN7android11MediaSource11ReadOptions11clearSeekToEv);
        addFunction(_ZN7android11MediaSource11ReadOptions9setSeekToEx);
        addFunction(_ZN7android11MediaSource11ReadOptions9setSeekToExNS1_8SeekModeE);
        addFunction(_ZN7android11MediaSource11ReadOptionsC1Ev);
        addFunction(_ZN7android11MediaSourceC2Ev);
        addFunction(_ZN7android11MediaSourceD0Ev);
        addFunction(_ZN7android11MediaSourceD1Ev);
        addFunction(_ZN7android11MediaSourceD2Ev);
        addFunction(_ZN7android11QueryCodecsERKNS_2spINS_4IOMXEEEPKcbPNS_6VectorINS_17CodecCapabilitiesEEE);
        addFunction(_ZN7android12IOMXObserverC2Ev);
        addFunction(_ZN7android12IOMXObserverD0Ev);
        addFunction(_ZN7android12IOMXObserverD1Ev);
        addFunction(_ZN7android12IOMXObserverD2Ev);
        addFunction(_ZN7android12MemoryDealerC1EjPKc);
        addFunction(_ZN7android12ProcessState15startThreadPoolEv);
        addFunction(_ZN7android12ProcessState4selfEv);
        addFunction(_ZN7android12SharedBuffer5allocEj);
        addFunction(_ZN7android12bitsPerPixelEi);
        addFunction(_ZN7android13BnOMXObserver10onTransactEjRKNS_6ParcelEPS1_j);
        addFunction(_ZN7android13GraphicBuffer4lockEjPPv);
        addFunction(_ZN7android13GraphicBuffer6unlockEv);
        addFunction(_ZN7android13GraphicBufferC1EP19ANativeWindowBufferb);
        addFunction(_ZN7android13GraphicBufferC1Ejjij);
        addFunction(_ZN7android13MediaProfiles11getInstanceEv);
        addFunction(_ZN7android14AndroidRuntime21registerNativeMethodsEP7_JNIEnvPKcPK15JNINativeMethodi);
        addFunction(_ZN7android14AndroidRuntime9getJNIEnvEv);
        addFunction(_ZN7android14IPCThreadState13flushCommandsEv);
        addFunction(_ZN7android14IPCThreadState4selfEv);
        addFunction(_ZN7android14SurfaceTexture14updateTexImageEv);
        addFunction(_ZN7android14SurfaceTexture18getTransformMatrixEPf);
        addFunction(_ZN7android14SurfaceTexture25setFrameAvailableListenerERKNS_2spINS0_22FrameAvailableListenerEEE);
        addFunction(_ZN7android14SurfaceTextureC1Ej);
        addFunction(_ZN7android16MediaBufferGroup10add_bufferEPNS_11MediaBufferE);
        addFunction(_ZN7android16MediaBufferGroup14acquire_bufferEPPNS_11MediaBufferE);
        addFunction(_ZN7android16MediaBufferGroupC1Ev);
        addFunction(_ZN7android16SortedVectorImpl25reservedSortedVectorImpl1Ev);
        addFunction(_ZN7android16SortedVectorImpl25reservedSortedVectorImpl2Ev);
        addFunction(_ZN7android16SortedVectorImpl25reservedSortedVectorImpl3Ev);
        addFunction(_ZN7android16SortedVectorImpl25reservedSortedVectorImpl4Ev);
        addFunction(_ZN7android16SortedVectorImpl25reservedSortedVectorImpl5Ev);
        addFunction(_ZN7android16SortedVectorImpl25reservedSortedVectorImpl6Ev);
        addFunction(_ZN7android16SortedVectorImpl25reservedSortedVectorImpl7Ev);
        addFunction(_ZN7android16SortedVectorImpl25reservedSortedVectorImpl8Ev);
        addFunction(_ZN7android16SortedVectorImpl3addEPKv);
        addFunction(_ZN7android16SortedVectorImplC2Ejj);
        addFunction(_ZN7android16SortedVectorImplD2Ev);
        addFunction(_ZN7android19GraphicBufferMapper14registerBufferEPK13native_handle);
        addFunction(_ZN7android19GraphicBufferMapper14registerBufferEPK15native_handle_t);
        addFunction(_ZN7android19GraphicBufferMapper16unregisterBufferEPK13native_handle);
        addFunction(_ZN7android19GraphicBufferMapper16unregisterBufferEPK15native_handle_t);
        addFunction(_ZN7android19GraphicBufferMapper4lockEPK15native_handle_tiRKNS_4RectEPPv);
        addFunction(_ZN7android19GraphicBufferMapper6unlockEPK15native_handle_t);
        addFunction(_ZN7android19GraphicBufferMapperC1Ev);
        addFunction(_ZN7android19GraphicBufferMapperC2Ev);
        addFunction(_ZN7android19GraphicBufferMapper6unlockEPK13native_handle);
        addFunction(_ZN7android19GraphicBufferMapper4lockEPK13native_handleiRKNS_4RectEPPv);
        addFunction(_ZN7android19IMediaDeathNotifier21getMediaPlayerServiceEv);
        addFunction(_ZN7android19IMediaPlayerService11asInterfaceERKNS_2spINS_7IBinderEEE);
        addFunction(_ZN7android19parcelForJavaObjectEP7_JNIEnvP8_jobject);
        addFunction(_ZN7android20SurfaceTextureClientC1ERKNS_2spINS_15ISurfaceTextureEEE);
        addFunction(_ZN7android20ibinderForJavaObjectEP7_JNIEnvP8_jobject);
        addFunction(_ZN7android20javaObjectForIBinderEP7_JNIEnvRKNS_2spINS_7IBinderEEE);
        addFunction(_ZN7android21defaultServiceManagerEv);
        addFunction(_ZN7android38android_SurfaceTexture_getNativeWindowEP7_JNIEnvP8_jobject);
        addFunction(_ZN7android4IOMX14createRendererERKNS_2spINS_7SurfaceEEEPKc20OMX_COLOR_FORMATTYPEjjjji);
        addFunction(_ZN7android4IOMX29createRendererFromJavaSurfaceEP7_JNIEnvP8_jobjectPKc20OMX_COLOR_FORMATTYPEjjjji);
        addFunction(_ZN7android6Parcel10writeFloatEf);
        addFunction(_ZN7android6Parcel10writeInt32Ei);
        addFunction(_ZN7android6Parcel10writeInt64Ex);
        addFunction(_ZN7android6Parcel11writeDoubleEd);
        addFunction(_ZN7android6Parcel12writeCStringEPKc);
        addFunction(_ZN7android6Parcel19writeInterfaceTokenERKNS_8String16E);
        addFunction(_ZN7android6Thread10readyToRunEv);
        addFunction(_ZN7android6Thread11requestExitEv);
        addFunction(_ZN7android6Thread18requestExitAndWaitEv);
        addFunction(_ZN7android6Thread3runEPKcij);
        addFunction(_ZN7android6ThreadC2Eb);
        addFunction(_ZN7android6ThreadD0Ev);
        addFunction(_ZN7android6ThreadD1Ev);
        addFunction(_ZN7android6ThreadD2Ev);
        addFunction(_ZN7android7BBinder10onTransactEjRKNS_6ParcelEPS1_j);
        addFunction(_ZN7android7BBinder10pingBinderEv);
        addFunction(_ZN7android7BBinder11linkToDeathERKNS_2spINS_7IBinder14DeathRecipientEEEPvj);
        addFunction(_ZN7android7BBinder11localBinderEv);
        addFunction(_ZN7android7BBinder12attachObjectEPKvPvS3_PFvS2_S3_S3_E);
        addFunction(_ZN7android7BBinder12detachObjectEPKv);
        addFunction(_ZN7android7BBinder13unlinkToDeathERKNS_2wpINS_7IBinder14DeathRecipientEEEPvjPS4_);
        addFunction(_ZN7android7BBinder4dumpEiRKNS_6VectorINS_8String16EEE);
        addFunction(_ZN7android7BBinder8transactEjRKNS_6ParcelEPS1_j);
        addFunction(_ZN7android7BBinderC2Ev);
        addFunction(_ZN7android7BBinderD0Ev);
        addFunction(_ZN7android7BBinderD1Ev);
        addFunction(_ZN7android7BBinderD2Ev);
        addFunction(_ZN7android7IBinder11localBinderEv);
        addFunction(_ZN7android7IBinder12remoteBinderEv);
        addFunction(_ZN7android7IBinder19queryLocalInterfaceERKNS_8String16E);
        addFunction(_ZN7android7IBinderD0Ev);
        addFunction(_ZN7android7IBinderD1Ev);
        addFunction(_ZN7android7IMemory11asInterfaceERKNS_2spINS_7IBinderEEE);
        addFunction(_ZN7android7RefBase10onFirstRefEv);
        addFunction(_ZN7android7RefBase12weakref_type16attemptIncStrongEPKv);
        addFunction(_ZN7android7RefBase12weakref_type7decWeakEPKv);
        addFunction(_ZN7android7RefBase12weakref_type7incWeakEPKv);
        addFunction(_ZN7android7RefBase13onLastWeakRefEPKv);
        addFunction(_ZN7android7RefBase15onLastStrongRefEPKv);
        addFunction(_ZN7android7RefBase20onIncStrongAttemptedEjPKv);
        addFunction(_ZN7android7RefBaseC2Ev);
        addFunction(_ZN7android7RefBaseD2Ev);
        addFunction(_ZN7android7String85setToEPKc);
        addFunction(_ZN7android7String85setToERKS0_);
        addFunction(_ZN7android7String86appendEPKc);
        addFunction(_ZN7android7String8C1EPKc);
        addFunction(_ZN7android7String8C1ERKS0_);
        addFunction(_ZN7android7String8C1Ev);
        addFunction(_ZN7android7String8D1Ev);
        addFunction(_ZN7android7Surface13unlockAndPostEv);
        addFunction(_ZN7android7Surface18setBuffersGeometryEiii);
        addFunction(_ZN7android7Surface4lockEPNS0_11SurfaceInfoEPNS_6RegionE);
        addFunction(_ZN7android7Surface4lockEPNS0_11SurfaceInfoEb);
        addFunction(_ZN7android7Surface7isValidEv);
        addFunction(_ZN7android7Surface8setUsageEj);
        addFunction(_ZN7android8MetaData10setCStringEjPKc);
        addFunction(_ZN7android8MetaData11findCStringEjPPKc);
        addFunction(_ZN7android8MetaData11findPointerEjPPv);
        addFunction(_ZN7android8MetaData5clearEv);
        addFunction(_ZN7android8MetaData6removeEj);
        addFunction(_ZN7android8MetaData7setDataEjjPKvj);
        addFunction(_ZN7android8MetaData8findRectEjPiS1_S1_S1_);
        addFunction(_ZN7android8MetaData8setInt32Eji);
        addFunction(_ZN7android8MetaData8setInt64Ejx);
        addFunction(_ZN7android8MetaData9findInt32EjPi);
        addFunction(_ZN7android8MetaData9findInt64EjPx);
        addFunction(_ZN7android8MetaDataC1Ev);
        addFunction(_ZN7android8OMXCodec16initOutputFormatERKNS_2spINS_8MetaDataEEE);
        addFunction(_ZN7android8OMXCodec22setVideoPortFormatTypeEm20OMX_VIDEO_CODINGTYPE20OMX_COLOR_FORMATTYPE);
        addFunction(_ZN7android8OMXCodec6CreateERKNS_2spINS_4IOMXEEERKNS1_INS_8MetaDataEEEbRKNS1_INS_11MediaSourceEEEPKcj);
        addFunction(_ZN7android8OMXCodec6CreateERKNS_2spINS_4IOMXEEERKNS1_INS_8MetaDataEEEbRKNS1_INS_11MediaSourceEEEPKcjRKNS1_I13ANativeWindowEE);
        addFunction(_ZN7android8OMXCodec8setStateENS0_5StateE);
        addFunction(_ZN7android8String16C1EPKc);
        addFunction(_ZN7android8String16D1Ev);
        addFunction(_ZN7android9OMXClient10disconnectEv);
        addFunction(_ZN7android9OMXClient7connectEv);
        addFunction(_ZN7android9OMXClientC1Ev);

        addFunction(_ZN8SkBitmap9setConfigENS_6ConfigEiii);
        addFunction(_ZN8SkBitmap9setPixelsEPvP12SkColorTable);
        addFunction(_ZN8SkBitmapC1Ev);
        addFunction(_ZN8SkBitmapD1Ev);
        addFunction(_ZN8SkCanvas14drawBitmapRectERK8SkBitmapPK7SkIRectRK6SkRectPK7SkPaint);
        addFunction(_ZN8SkCanvas15setBitmapDeviceERK8SkBitmap);
        addFunction(_ZN8SkCanvas15setBitmapDeviceERK8SkBitmapb);
        addFunction(_ZN8SkCanvas9drawColorEjN10SkXfermode4ModeE);
        addFunction(_ZN8SkCanvas9drawColorEjN12SkPorterDuff4ModeE);
        addFunction(_ZN8SkCanvasC1EP15SkDeviceFactory);
        addFunction(_ZN8SkCanvasC1EP8SkDevice);
        addFunction(_ZN8SkCanvasC1Ev);
        addFunction(_ZN8SkCanvasD1Ev);
        addFunction(_ZNK7android10AudioTrack10frameCountEv);
        addFunction(_ZNK7android10AudioTrack12channelCountEv);
        addFunction(_ZNK7android10AudioTrack4dumpEiRKNS_6VectorINS_8String16EEE);
        addFunction(_ZNK7android10AudioTrack7latencyEv);
        addFunction(_ZNK7android10AudioTrack9frameSizeEv);
        addFunction(_ZNK7android10AudioTrack9initCheckEv);
        addFunction(_ZNK7android11MediaBuffer12range_lengthEv);
        addFunction(_ZNK7android11MediaBuffer12range_offsetEv);
        addFunction(_ZNK7android11MediaBuffer13graphicBufferEv);
        addFunction(_ZNK7android11MediaBuffer4dataEv);
        addFunction(_ZNK7android11MediaBuffer4sizeEv);
        addFunction(_ZNK7android11MediaBuffer8refcountEv);
        addFunction(_ZNK7android11MediaSource11ReadOptions9getSeekToEPxPNS1_8SeekModeE);
        addFunction(_ZNK7android12IOMXObserver22getInterfaceDescriptorEv);
        addFunction(_ZNK7android12SharedBuffer10editResizeEj);
        addFunction(_ZNK7android12SharedBuffer4editEv);
        addFunction(_ZNK7android12SharedBuffer7acquireEv);
        addFunction(_ZNK7android12SharedBuffer7releaseEj);
        addFunction(_ZNK7android13GraphicBuffer15getNativeBufferEv);
        addFunction(_ZNK7android13GraphicBuffer9initCheckEv);
        addFunction(_ZNK7android13MediaProfiles28getVideoEditorCapParamByNameEPKc);
        addFunction(_ZNK7android13MediaProfiles31getVideoEditorExportParamByNameEPKci);
        addFunction(_ZNK7android16SortedVectorImpl7indexOfEPKv);
        addFunction(_ZNK7android6Parcel10readDoubleEv);
        addFunction(_ZNK7android6Parcel11readCStringEv);
        addFunction(_ZNK7android6Parcel12dataPositionEv);
        addFunction(_ZNK7android6Parcel14checkInterfaceEPNS_7IBinderE);
        addFunction(_ZNK7android6Parcel19readString16InplaceEPj);
        addFunction(_ZNK7android6Parcel4readEPvj);
        addFunction(_ZNK7android6Parcel9readFloatEv);
        addFunction(_ZNK7android6Parcel9readInt32EPi);
        addFunction(_ZNK7android6Parcel9readInt32Ev);
        addFunction(_ZNK7android6Parcel9readInt64Ev);
        addFunction(_ZNK7android7BBinder10findObjectEPKv);
        addFunction(_ZNK7android7BBinder13isBinderAliveEv);
        addFunction(_ZNK7android7BBinder22getInterfaceDescriptorEv);
        addFunction(_ZNK7android7IBinder13checkSubclassEPKv);
        addFunction(_ZNK7android7IMemory4sizeEv);
        addFunction(_ZNK7android7IMemory6offsetEv);
        addFunction(_ZNK7android7IMemory7pointerEv);
        addFunction(_ZNK7android7RefBase10createWeakEPKv);
        addFunction(_ZNK7android7RefBase9decStrongEPKv);
        addFunction(_ZNK7android7RefBase9incStrongEPKv);
        addFunction(_ZNK7android7Surface11getISurfaceEv);
        addFunction(_ZNK7android8MetaData8findDataEjPjPPKvS1_);
        addFunction(_ZNK7android8MetaData9dumpToLogEv);
        addFunction(_ZNK8SkBitmap10lockPixelsEv);
        addFunction(_ZNK8SkBitmap12unlockPixelsEv);
//        addFunction(_ZNSt12__node_alloc11_M_allocateERj);
        addFunction(_ZNSt12__node_alloc13_M_deallocateEPvj);
        addFunction(_ZThn4_N7android13BnOMXObserver10onTransactEjRKNS_6ParcelEPS1_j);
        addFunction(_ZTv0_n12_N7android10IInterfaceD0Ev);
        addFunction(_ZTv0_n12_N7android10IInterfaceD1Ev);
        addFunction(_ZTv0_n12_N7android11MediaSourceD0Ev);
        addFunction(_ZTv0_n12_N7android11MediaSourceD1Ev);
        addFunction(_ZTv0_n12_N7android12IOMXObserverD0Ev);
        addFunction(_ZTv0_n12_N7android12IOMXObserverD1Ev);
        addFunction(_ZTv0_n12_N7android6ThreadD0Ev);
        addFunction(_ZTv0_n12_N7android6ThreadD1Ev);
        addFunction(_ZTv0_n12_N7android7BBinderD0Ev);
        addFunction(_ZTv0_n12_N7android7BBinderD1Ev);
        addFunction(_ZTv0_n12_N7android7IBinderD0Ev);
        addFunction(_ZTv0_n12_N7android7IBinderD1Ev);
        addFunction(_ZdlPvRKSt9nothrow_t);
        addFunction(_ZnwjRKSt9nothrow_t);
        addFunction(__aeabi_atexit);
        addFunction(__aeabi_d2f);
        addFunction(__aeabi_d2iz);
        addFunction(__aeabi_d2lz);
        addFunction(__aeabi_d2ulz);
        addFunction(__aeabi_dadd);
        addFunction(__aeabi_dcmpeq);
        addFunction(__aeabi_dcmpge);
        addFunction(__aeabi_dcmpgt);
        addFunction(__aeabi_dcmple);
        addFunction(__aeabi_dcmplt);
        addFunction(__aeabi_dcmpun);
        addFunction(__aeabi_ddiv);
        addFunction(__aeabi_dmul);
        addFunction(__aeabi_dsub);
        addFunction(__aeabi_f2d);
        addFunction(__aeabi_f2iz);
        addFunction(__aeabi_f2uiz);
        addFunction(__aeabi_fadd);
        addFunction(__aeabi_fcmpeq);
        addFunction(__aeabi_fcmpge);
        addFunction(__aeabi_fcmpgt);
        addFunction(__aeabi_fcmple);
        addFunction(__aeabi_fcmplt);
        addFunction(__aeabi_fcmpun);
        addFunction(__aeabi_fdiv);
        addFunction(__aeabi_fmul);
        addFunction(__aeabi_fsub);
        addFunction(__aeabi_i2d);
        addFunction(__aeabi_i2f);
        addFunction(__aeabi_idiv);
        addFunction(__aeabi_idivmod);
        addFunction(__aeabi_l2d);
        addFunction(__aeabi_l2f);
        addFunction(__aeabi_ldivmod);
        addFunction(__aeabi_lmul);
        addFunction(__aeabi_memclr);
        addFunction(__aeabi_memclr4);
        addFunction(__aeabi_memcpy4);
        addFunction(__aeabi_memmove);
        addFunction(__aeabi_memmove4);
        addFunction(__aeabi_ui2d);
        addFunction(__aeabi_ui2f);
        addFunction(__aeabi_uidiv);
        addFunction(__aeabi_uidivmod);
        addFunction(__aeabi_ul2f);
        addFunction(__aeabi_uldivmod);
        addFunction(__aeabi_unwind_cpp_pr0);
        addFunction(__aeabi_unwind_cpp_pr1);
        addFunction(__android_log_assert);
        addFunction(__android_log_print);
        addFunction(__android_log_vprint);
        addFunction(__android_log_write);
        addFunction(__assert);
        addFunction(__assert2);
        addFunction(__atomic_cmpxchg);
        addFunction(__atomic_dec);
        addFunction(__atomic_inc);
        addFunction(__b64_ntop);
        addFunction(__cxa_atexit);
        addFunction(__cxa_begin_cleanup);
        addFunction(__cxa_type_match);
        addFunction(__cxa_finalize);
        addFunction(__cxa_guard_acquire);
        addFunction(__cxa_guard_release);
        addFunction(__cxa_pure_virtual);
        addFunction(__deregister_frame_info);
        addFunction(__register_frame_info);
        addFunction(__div0);
        addFunction(__fork);
        addFunction(__fpclassifyd);
        addFunction(__fpclassifyf);
        addFunction(__futex_wait);
        addFunction(__futex_wake);
        addFunction(__get_h_errno);
        addFunction(__get_thread);
        addFunction(__gnu_Unwind_Find_exidx);
        addFunction(__isfinite);
        addFunction(__isfinitef);
        addFunction(__isinf);
        addFunction(__isinff);
        addFunction(__isnanl);
        addFunction(__isnormal);
        addFunction(__libc_init);
        addFunction(__open_2);
        addFunction(__pthread_cleanup_pop);
        addFunction(__pthread_cleanup_push);
        addFunction(__set_tls);
        addFunction(__signbit);
        addFunction(__signbitf);
        addFunction(__signbitl);
        addFunction(__srefill);
        addFunction(__srget);
        addFunction(__strcat_chk);
        addFunction(__strcpy_chk);
        addFunction(__strlen_chk);
        addFunction(__strchr_chk);
        addFunction(__swbuf);
        addFunction(__swsetup);
        addFunction(__system_property_find);
        addFunction(__system_property_find_nth);
        addFunction(__system_property_get);
        addFunction(__system_property_read);
        addFunction(__wait4);
        addFunction(_exit);
        addFunction(_longjmp);
        addFunction(_setjmp);
        addFunction(__FD_SET_chk);
        addFunction(__memcpy_chk);
        addFunction(__sprintf_chk);
        addFunction(accept);
        addFunction(access);
        addFunction(acos);
        addFunction(acosf);
        addFunction(acosh);
        addFunction(acoshf);
        addFunction(adler32);
        addFunction(alarm);
        addFunction(alphasort);
        addFunction(androidCreateThread);
        addFunction(androidGetTid);
        addFunction(android_atomic_add);
        addFunction(android_atomic_cmpxchg);
        addFunction(android_atomic_dec);
        addFunction(android_atomic_inc);
        addFunction(android_atomic_or);
        addFunction(android_atomic_release_cas);
        addFunction(arc4random);
        addFunction(arc4random_uniform);
        addFunction(asctime);
        addFunction(ashmem_create_region);
        addFunction(ashmem_set_prot_region);
        addFunction(asin);
        addFunction(asinf);
        addFunction(asinh);
        addFunction(asinhf);
        addFunction(atan);
        addFunction(atan2);
        addFunction(atan2f);
        addFunction(atanf);
        addFunction(atanh);
        addFunction(atanhf);
        addFunction(atexit);
        addFunction(atoi);
        addFunction(atol);
        addFunction(atoll);
        addFunction(basename);
        addFunction(bcopy);
        addFunction(bind);
        addFunction(bsd_signal);
        addFunction(bsearch);
        addFunction(btowc);
        addFunction(bzero);
        addFunction(cacheflush);
        addFunction(cbrt);
        addFunction(cbrtf);
        addFunction(ceil);
        addFunction(ceilf);
        addFunction(chdir);
        addFunction(chmod);
        addFunction(chown);
        addFunction(clearerr);
        addFunction(clock);
        addFunction(clock_getres);
        addFunction(clock_gettime);
        addFunction(getReplacement);
        addFunction(closedir);
        addFunction(closelog);
        addFunction(compress);
        addFunction(compress2);
        addFunction(compressBound);
        addFunction(connect);
        addFunction(copysign);
        addFunction(copysignf);
        addFunction(cos);
        addFunction(cosf);
        addFunction(cosh);
        addFunction(coshf);
        addFunction(crc32);
        addFunction(creat);
        addFunction(ctime);
        addFunction(ctime_r);
        addFunction(d2i_PKCS7);
        addFunction(d2i_PKCS7_fp);
        addFunction(d2i_RSAPrivateKey);
        addFunction(d2i_RSAPublicKey);
        addFunction(d2i_X509);
        addFunction(deflate);
        addFunction(deflateBound);
        addFunction(deflateEnd);
        addFunction(deflateInit2_);
        addFunction(deflateInit_);
        addFunction(deflateParams);
        addFunction(deflateReset);
        addFunction(deflateSetDictionary);
        addFunction(difftime);
        addFunction(dirfd);
        addFunction(dirname);
        addFunction(dirname_r);
        addFunction(div);
        addFunction(dl_unwind_find_exidx);
        addFunction(dladdr);
        addFunction(dlerror);
        addFunction(dup);
        addFunction(dup2);
        addFunction(eglBindAPI);
        addFunction(eglChooseConfig);
        addFunction(eglCreateContext);
        addFunction(eglCreateImageKHR);
        addFunction(eglCreatePbufferSurface);
        addFunction(eglCreateWindowSurface);
        addFunction(eglDestroyContext);
        addFunction(eglDestroyImageKHR);
        addFunction(eglDestroySurface);
        addFunction(eglGetConfigAttrib);
        addFunction(eglGetConfigs);
        addFunction(eglGetCurrentContext);
        addFunction(eglGetCurrentDisplay);
        addFunction(eglGetCurrentSurface);
        addFunction(eglGetDisplay);
        addFunction(eglGetError);
        addFunction(eglGetProcAddress);
        addFunction(eglInitialize);
        addFunction(eglMakeCurrent);
        addFunction(eglQueryContext);
        addFunction(eglQueryString);
        addFunction(eglQuerySurface);
        addFunction(eglSurfaceAttrib);
        addFunction(eglSwapBuffers);
        addFunction(eglSwapInterval);
        addFunction(eglTerminate);
        addFunction(epoll_create);
        addFunction(epoll_ctl);
        addFunction(epoll_wait);
        addFunction(erf);
        addFunction(erfc);
        addFunction(erfcf);
        addFunction(erff);
        addFunction(execl);
        addFunction(execle);
        addFunction(execlp);
        addFunction(execv);
        addFunction(execve);
        addFunction(execvp);
        addFunction(exit);
        addFunction(exp);
        addFunction(exp2);
        addFunction(exp2f);
        addFunction(expf);
        addFunction(expm1);
        addFunction(expm1f);
        addFunction(fabs);
        addFunction(fabsf);
        addFunction(fchdir);
        addFunction(fchmod);
        addFunction(fchmodat);
        addFunction(fchown);
        addFunction(fcntl);
        addFunction(fdatasync);
        addFunction(fdimf);
        addFunction(fdopen);
        addFunction(feof);
        addFunction(ferror);
        addFunction(fflush);
        addFunction(fgetc);
        addFunction(fgetpos);
        addFunction(fgets);
        addFunction(fileno);
        addFunction(flock);
        addFunction(flockfile);
        addFunction(floor);
        addFunction(floorf);
        addFunction(fmaf);
        addFunction(fmax);
        addFunction(fmaxf);
        addFunction(fmin);
        addFunction(fminf);
        addFunction(fmod);
        addFunction(fmodf);
        addFunction(fnmatch);
        addFunction(fork);
        addFunction(fprintf);
        addFunction(fputc);
        addFunction(fputs);
        addFunction(fread);
        addFunction(freeaddrinfo);
        addFunction(freopen);
        addFunction(frexp);
        addFunction(frexpf);
        addFunction(fscanf);
        addFunction(fseek);
        addFunction(fseeko);
        addFunction(fsetpos);
        addFunction(fstat);
        addFunction(fstatat);
        addFunction(fstatfs);
        addFunction(fsync);
        addFunction(ftell);
        addFunction(ftello);
        addFunction(ftime);
        addFunction(ftruncate);
        addFunction(fts_children);
        addFunction(fts_close);
        addFunction(fts_open);
        addFunction(fts_read);
        addFunction(funlockfile);
        addFunction(funopen);
        addFunction(fwide);
        addFunction(fwprintf);
        addFunction(gai_strerror);
        addFunction(get_crc_table);
        addFunction(get_malloc_leak_info);
        addFunction(getaddrinfo);
        addFunction(getc);
        addFunction(getcwd);
        addFunction(getdents);
        addFunction(getdtablesize);
        addFunction(getegid);
        addFunction(getenv);
        addFunction(geteuid);
        addFunction(getgid);
        addFunction(getgrgid);
        addFunction(getgrnam);
        addFunction(gethostbyaddr);
        addFunction(gethostbyname);
        addFunction(gethostbyname2);
        addFunction(gethostbyname_r);
        addFunction(gethostname);
        addFunction(getitimer);
        addFunction(getlogin);
        addFunction(getnameinfo);
        addFunction(getopt);
        addFunction(getopt_long);
        addFunction(getpeername);
        addFunction(getppid);
        addFunction(getpriority);
        addFunction(getprotobyname);
        addFunction(getpwnam);
        addFunction(getpwuid);
        addFunction(getresuid);
        addFunction(getrlimit);
        addFunction(getrusage);
        addFunction(getservbyname);
        addFunction(getsockname);
        addFunction(getsockopt);
        addFunction(gettid);
        addFunction(gettimeofday);
        addFunction(getuid);
        addFunction(getwc);
        addFunction(glActiveTexture);
        addFunction(glAlphaFunc);
        addFunction(glAlphaFuncx);
        addFunction(glAlphaFuncxOES);
        addFunction(glAttachShader);
        addFunction(glBeginPerfMonitorAMD);
        addFunction(glBindAttribLocation);
        addFunction(glBindBuffer);
        addFunction(glBindFramebuffer);
        addFunction(glBindFramebufferOES);
        addFunction(glBindRenderbuffer);
        addFunction(glBindRenderbufferOES);
        addFunction(glBindTexture);
        addFunction(glBlendColor);
        addFunction(glBlendEquation);
        addFunction(glBlendEquationOES);
        addFunction(glBlendEquationSeparate);
        addFunction(glBlendEquationSeparateOES);
        addFunction(glBlendFunc);
        addFunction(glBlendFuncSeparate);
        addFunction(glBlendFuncSeparateOES);
        addFunction(glBufferData);
        addFunction(glBufferSubData);
        addFunction(glCheckFramebufferStatus);
        addFunction(glCheckFramebufferStatusOES);
        addFunction(glClear);
        addFunction(glClearColor);
        addFunction(glClearColorx);
        addFunction(glClearColorxOES);
        addFunction(glClearDepthf);
        addFunction(glClearDepthfOES);
        addFunction(glClearDepthx);
        addFunction(glClearDepthxOES);
        addFunction(glClearStencil);
        addFunction(glClientActiveTexture);
        addFunction(glClipPlanef);
        addFunction(glClipPlanefOES);
        addFunction(glClipPlanex);
        addFunction(glClipPlanexOES);
        addFunction(glColor4f);
        addFunction(glColor4ub);
        addFunction(glColor4x);
        addFunction(glColor4xOES);
        addFunction(glColorMask);
        addFunction(glColorPointer);
        addFunction(glCompileShader);
        addFunction(glCompressedTexImage2D);
        addFunction(glCompressedTexImage3DOES);
        addFunction(glCompressedTexSubImage2D);
        addFunction(glCompressedTexSubImage3DOES);
        addFunction(glCopyTexImage2D);
        addFunction(glCopyTexSubImage2D);
        addFunction(glCopyTexSubImage3DOES);
        addFunction(glCreateProgram);
        addFunction(glCreateShader);
        addFunction(glCullFace);
        addFunction(glCurrentPaletteMatrixOES);
        addFunction(glDeleteBuffers);
        addFunction(glDeleteFencesNV);
        addFunction(glDeleteFramebuffers);
        addFunction(glDeleteFramebuffersOES);
        addFunction(glDeletePerfMonitorsAMD);
        addFunction(glDeleteProgram);
        addFunction(glDeleteRenderbuffers);
        addFunction(glDeleteRenderbuffersOES);
        addFunction(glDeleteShader);
        addFunction(glDeleteTextures);
        addFunction(glDepthFunc);
        addFunction(glDepthMask);
        addFunction(glDepthRangef);
        addFunction(glDepthRangefOES);
        addFunction(glDepthRangex);
        addFunction(glDepthRangexOES);
        addFunction(glDetachShader);
        addFunction(glDisable);
        addFunction(glDisableClientState);
        addFunction(glDisableDriverControlQCOM);
        addFunction(glDisableVertexAttribArray);
        addFunction(glDrawArrays);
        addFunction(glDrawElements);
        addFunction(glDrawTexfOES);
        addFunction(glDrawTexfvOES);
        addFunction(glDrawTexiOES);
        addFunction(glDrawTexivOES);
        addFunction(glDrawTexsOES);
        addFunction(glDrawTexsvOES);
        addFunction(glDrawTexxOES);
        addFunction(glDrawTexxvOES);
        addFunction(glEGLImageTargetRenderbufferStorageOES);
        addFunction(glEGLImageTargetTexture2DOES);
        addFunction(glEnable);
        addFunction(glEnableClientState);
        addFunction(glEnableDriverControlQCOM);
        addFunction(glEnableVertexAttribArray);
        addFunction(glEndPerfMonitorAMD);
        addFunction(glFinish);
        addFunction(glFinishFenceNV);
        addFunction(glFlush);
        addFunction(glFogf);
        addFunction(glFogfv);
        addFunction(glFogx);
        addFunction(glFogxOES);
        addFunction(glFogxv);
        addFunction(glFogxvOES);
        addFunction(glFramebufferRenderbuffer);
        addFunction(glFramebufferRenderbufferOES);
        addFunction(glFramebufferTexture2D);
        addFunction(glFramebufferTexture2DOES);
        addFunction(glFramebufferTexture3DOES);
        addFunction(glFrontFace);
        addFunction(glFrustumf);
        addFunction(glFrustumfOES);
        addFunction(glFrustumx);
        addFunction(glFrustumxOES);
        addFunction(glGenBuffers);
        addFunction(glGenFencesNV);
        addFunction(glGenFramebuffers);
        addFunction(glGenFramebuffersOES);
        addFunction(glGenPerfMonitorsAMD);
        addFunction(glGenRenderbuffers);
        addFunction(glGenRenderbuffersOES);
        addFunction(glGenTextures);
        addFunction(glGenerateMipmap);
        addFunction(glGenerateMipmapOES);
        addFunction(glGetActiveAttrib);
        addFunction(glGetActiveUniform);
        addFunction(glGetAttachedShaders);
        addFunction(glGetAttribLocation);
        addFunction(glGetBooleanv);
        addFunction(glGetBufferParameteriv);
        addFunction(glGetBufferPointervOES);
        addFunction(glGetClipPlanef);
        addFunction(glGetClipPlanefOES);
        addFunction(glGetClipPlanex);
        addFunction(glGetClipPlanexOES);
        addFunction(glGetDriverControlStringQCOM);
        addFunction(glGetDriverControlsQCOM);
        addFunction(glGetError);
        addFunction(glGetFenceivNV);
        addFunction(glGetFixedv);
        addFunction(glGetFixedvOES);
        addFunction(glGetFloatv);
        addFunction(glGetFramebufferAttachmentParameteriv);
        addFunction(glGetFramebufferAttachmentParameterivOES);
        addFunction(glGetIntegerv);
        addFunction(glGetLightfv);
        addFunction(glGetLightxv);
        addFunction(glGetLightxvOES);
        addFunction(glGetMaterialfv);
        addFunction(glGetMaterialxv);
        addFunction(glGetMaterialxvOES);
        addFunction(glGetPerfMonitorCounterDataAMD);
        addFunction(glGetPerfMonitorCounterInfoAMD);
        addFunction(glGetPerfMonitorCounterStringAMD);
        addFunction(glGetPerfMonitorCountersAMD);
        addFunction(glGetPerfMonitorGroupStringAMD);
        addFunction(glGetPerfMonitorGroupsAMD);
        addFunction(glGetPointerv);
        addFunction(glGetProgramBinaryOES);
        addFunction(glGetProgramInfoLog);
        addFunction(glGetProgramiv);
        addFunction(glGetRenderbufferParameteriv);
        addFunction(glGetRenderbufferParameterivOES);
        addFunction(glGetShaderInfoLog);
        addFunction(glGetShaderPrecisionFormat);
        addFunction(glGetShaderSource);
        addFunction(glGetShaderiv);
        addFunction(glGetString);
        addFunction(glGetTexEnvfv);
        addFunction(glGetTexEnviv);
        addFunction(glGetTexEnvxv);
        addFunction(glGetTexEnvxvOES);
        addFunction(glGetTexGenfvOES);
        addFunction(glGetTexGenivOES);
        addFunction(glGetTexGenxvOES);
        addFunction(glGetTexParameterfv);
        addFunction(glGetTexParameteriv);
        addFunction(glGetTexParameterxv);
        addFunction(glGetTexParameterxvOES);
        addFunction(glGetUniformLocation);
        addFunction(glGetUniformfv);
        addFunction(glGetUniformiv);
        addFunction(glGetVertexAttribPointerv);
        addFunction(glGetVertexAttribfv);
        addFunction(glGetVertexAttribiv);
        addFunction(glHint);
        addFunction(glIsBuffer);
        addFunction(glIsEnabled);
        addFunction(glIsFenceNV);
        addFunction(glIsFramebuffer);
        addFunction(glIsFramebufferOES);
        addFunction(glIsProgram);
        addFunction(glIsRenderbuffer);
        addFunction(glIsRenderbufferOES);
        addFunction(glIsShader);
        addFunction(glIsTexture);
        addFunction(glLightModelf);
        addFunction(glLightModelfv);
        addFunction(glLightModelx);
        addFunction(glLightModelxOES);
        addFunction(glLightModelxv);
        addFunction(glLightModelxvOES);
        addFunction(glLightf);
        addFunction(glLightfv);
        addFunction(glLightx);
        addFunction(glLightxOES);
        addFunction(glLightxv);
        addFunction(glLightxvOES);
        addFunction(glLineWidth);
        addFunction(glLineWidthx);
        addFunction(glLineWidthxOES);
        addFunction(glLinkProgram);
        addFunction(glLoadIdentity);
        addFunction(glLoadMatrixf);
        addFunction(glLoadMatrixx);
        addFunction(glLoadMatrixxOES);
        addFunction(glLoadPaletteFromModelViewMatrixOES);
        addFunction(glLogicOp);
        addFunction(glMapBufferOES);
        addFunction(glMaterialf);
        addFunction(glMaterialfv);
        addFunction(glMaterialx);
        addFunction(glMaterialxOES);
        addFunction(glMaterialxv);
        addFunction(glMaterialxvOES);
        addFunction(glMatrixIndexPointerOES);
        addFunction(glMatrixMode);
        addFunction(glMultMatrixf);
        addFunction(glMultMatrixx);
        addFunction(glMultMatrixxOES);
        addFunction(glMultiTexCoord4f);
        addFunction(glMultiTexCoord4x);
        addFunction(glMultiTexCoord4xOES);
        addFunction(glNormal3f);
        addFunction(glNormal3x);
        addFunction(glNormal3xOES);
        addFunction(glNormalPointer);
        addFunction(glOrthof);
        addFunction(glOrthofOES);
        addFunction(glOrthox);
        addFunction(glOrthoxOES);
        addFunction(glPixelStorei);
        addFunction(glPointParameterf);
        addFunction(glPointParameterfv);
        addFunction(glPointParameterx);
        addFunction(glPointParameterxOES);
        addFunction(glPointParameterxv);
        addFunction(glPointParameterxvOES);
        addFunction(glPointSize);
        addFunction(glPointSizePointerOES);
        addFunction(glPointSizex);
        addFunction(glPointSizexOES);
        addFunction(glPolygonOffset);
        addFunction(glPolygonOffsetx);
        addFunction(glPolygonOffsetxOES);
        addFunction(glPopMatrix);
        addFunction(glProgramBinaryOES);
        addFunction(glPushMatrix);
        addFunction(glQueryMatrixxOES);
        addFunction(glReadPixels);
        addFunction(glReleaseShaderCompiler);
        addFunction(glRenderbufferStorage);
        addFunction(glRenderbufferStorageOES);
        addFunction(glRotatef);
        addFunction(glRotatex);
        addFunction(glRotatexOES);
        addFunction(glSampleCoverage);
        addFunction(glSampleCoveragex);
        addFunction(glSampleCoveragexOES);
        addFunction(glScalef);
        addFunction(glScalex);
        addFunction(glScalexOES);
        addFunction(glScissor);
        addFunction(glSelectPerfMonitorCountersAMD);
        addFunction(glSetFenceNV);
        addFunction(glShadeModel);
        addFunction(glShaderBinary);
        addFunction(glShaderSource);
        addFunction(glStencilFunc);
        addFunction(glStencilFuncSeparate);
        addFunction(glStencilMask);
        addFunction(glStencilMaskSeparate);
        addFunction(glStencilOp);
        addFunction(glStencilOpSeparate);
        addFunction(glTestFenceNV);
        addFunction(glTexCoordPointer);
        addFunction(glTexEnvf);
        addFunction(glTexEnvfv);
        addFunction(glTexEnvi);
        addFunction(glTexEnviv);
        addFunction(glTexEnvx);
        addFunction(glTexEnvxOES);
        addFunction(glTexEnvxv);
        addFunction(glTexEnvxvOES);
        addFunction(glTexGenfOES);
        addFunction(glTexGenfvOES);
        addFunction(glTexGeniOES);
        addFunction(glTexGenivOES);
        addFunction(glTexGenxOES);
        addFunction(glTexGenxvOES);
        addFunction(glTexImage2D);
        addFunction(glTexImage3DOES);
        addFunction(glTexParameterf);
        addFunction(glTexParameterfv);
        addFunction(glTexParameteri);
        addFunction(glTexParameteriv);
        addFunction(glTexParameterx);
        addFunction(glTexParameterxOES);
        addFunction(glTexParameterxv);
        addFunction(glTexParameterxvOES);
        addFunction(glTexSubImage2D);
        addFunction(glTexSubImage3DOES);
        addFunction(glTranslatef);
        addFunction(glTranslatex);
        addFunction(glTranslatexOES);
        addFunction(glUniform1f);
        addFunction(glUniform1fv);
        addFunction(glUniform1i);
        addFunction(glUniform1iv);
        addFunction(glUniform2f);
        addFunction(glUniform2fv);
        addFunction(glUniform2i);
        addFunction(glUniform2iv);
        addFunction(glUniform3f);
        addFunction(glUniform3fv);
        addFunction(glUniform3i);
        addFunction(glUniform3iv);
        addFunction(glUniform4f);
        addFunction(glUniform4fv);
        addFunction(glUniform4i);
        addFunction(glUniform4iv);
        addFunction(glUniformMatrix2fv);
        addFunction(glUniformMatrix3fv);
        addFunction(glUniformMatrix4fv);
        addFunction(glUnmapBufferOES);
        addFunction(glUseProgram);
        addFunction(glValidateProgram);
        addFunction(glVertexAttrib1f);
        addFunction(glVertexAttrib1fv);
        addFunction(glVertexAttrib2f);
        addFunction(glVertexAttrib2fv);
        addFunction(glVertexAttrib3f);
        addFunction(glVertexAttrib3fv);
        addFunction(glVertexAttrib4f);
        addFunction(glVertexAttrib4fv);
        addFunction(glVertexAttribPointer);
        addFunction(glVertexPointer);
        addFunction(glViewport);
        addFunction(glWeightPointerOES);
        addFunction(gmtime);
        addFunction(gmtime64_r);
        addFunction(gmtime_r);
        addFunction(gzclose);
        addFunction(gzeof);
        addFunction(gzerror);
        addFunction(gzgets);
        addFunction(gzopen);
        addFunction(gzputs);
        addFunction(gzread);
        addFunction(gzrewind);
        addFunction(gzwrite);
        addFunction(hstrerror);
        addFunction(hypot);
        addFunction(hypotf);
        addFunction(i2a_ASN1_OBJECT);
        addFunction(i2d_PublicKey);
        addFunction(i2d_RSAPrivateKey);
        addFunction(i2d_RSAPublicKey);
        addFunction(if_indextoname);
        addFunction(if_nametoindex);
        addFunction(ilogbf);
        addFunction(index);
        addFunction(inet_addr);
        addFunction(inet_aton);
        addFunction(inet_nsap_addr);
        addFunction(inet_nsap_ntoa);
        addFunction(inet_ntoa);
        addFunction(inet_ntop);
        addFunction(inet_pton);
        addFunction(inflate);
        addFunction(inflateCopy);
        addFunction(inflateEnd);
        addFunction(inflateInit2_);
        addFunction(inflateInit_);
        addFunction(inflateReset);
        addFunction(inflateSetDictionary);
        addFunction(inflateSync);
        addFunction(initgroups);
        addFunction(inotify_add_watch);
        addFunction(inotify_init);
        addFunction(inotify_rm_watch);
        addFunction(isalnum);
        addFunction(isalpha);
        addFunction(isatty);
        addFunction(iscntrl);
        addFunction(isdigit);
        addFunction(isgraph);
        addFunction(islower);
        addFunction(isnan);
        addFunction(isnanf);
        addFunction(isprint);
        addFunction(ispunct);
        addFunction(issetugid);
        addFunction(isspace);
        addFunction(isupper);
        addFunction(iswalnum);
        addFunction(iswalpha);
        addFunction(iswcntrl);
        addFunction(iswctype);
        addFunction(iswdigit);
        addFunction(iswlower);
        addFunction(iswprint);
        addFunction(iswpunct);
        addFunction(iswspace);
        addFunction(iswupper);
        addFunction(iswxdigit);
        addFunction(isxdigit);
        addFunction(jniThrowException);
        addFunction(jrand48);
        addFunction(kill);
        addFunction(klogctl);
        addFunction(lchown);
        addFunction(ldexp);
        addFunction(ldexpf);
        addFunction(ldiv);
        addFunction(lgamma);
        addFunction(lgammaf);
        addFunction(lgammaf_r);
        addFunction(link);
        addFunction(listen);
        addFunction(llrint);
        addFunction(llrintf);
        addFunction(localtime);
        addFunction(localtime64_r);
        addFunction(localtime_r);
        addFunction(log);
        addFunction(log10);
        addFunction(log10f);
        addFunction(log1p);
        addFunction(log1pf);
        addFunction(logb);
        addFunction(logbf);
        addFunction(logf);
        addFunction(longjmp);
        addFunction(lrint);
        addFunction(lrintf);
        addFunction(lround);
        addFunction(lroundf);
        addFunction(lseek);
        addFunction(lseek64);
        addFunction(lstat);
        addFunction(madvise);
        addFunction(mallinfo);
        addFunction(mbrtowc);
        addFunction(mbsrtowcs);
        addFunction(mbstowcs);
        addFunction(memalign);
        addFunction(memccpy);
        addFunction(memchr);
        addFunction(memcmp);
        addFunction(memmem);
        addFunction(memrchr);
        addFunction(memswap);
        addFunction(mincore);
        addFunction(mkdir);
        addFunction(mkdtemp);
        addFunction(mknod);
        addFunction(mkstemp);
        addFunction(mktemp);
        addFunction(mktime);
        addFunction(mktime64);
        addFunction(mlock);
        addFunction(mmap);
        addFunction(modf);
        addFunction(modff);
        addFunction(mprotect);
        addFunction(mrand48);
        addFunction(seed48);
        addFunction(mremap);
        addFunction(msync);
        addFunction(munlock);
        addFunction(munmap);
        addFunction(nanosleep);
        addFunction(nearbyint);
        addFunction(nextafter);
        addFunction(nextafterf);
        addFunction(nice);
        addFunction(nsdispatch);
        addFunction(openat);
        addFunction(opendir);
        addFunction(openlog);
        addFunction(pathconf);
        addFunction(pause);
        addFunction(pclose);
        addFunction(pipe);
        addFunction(pipe2);
        addFunction(poll);
        addFunction(popen);
        addFunction(pow);
        addFunction(powf);
        addFunction(prctl);
        addFunction(pread);
        addFunction(property_get);
        addFunction(pselect);
        addFunction(pthread_attr_destroy);
        addFunction(pthread_attr_getdetachstate);
        addFunction(pthread_attr_getguardsize);
        addFunction(pthread_attr_getschedparam);
        addFunction(pthread_attr_getschedpolicy);
        addFunction(pthread_attr_getstack);
        addFunction(pthread_attr_getstackaddr);
        addFunction(pthread_attr_getstacksize);
        addFunction(pthread_attr_init);
        addFunction(pthread_attr_setdetachstate);
        addFunction(pthread_attr_setguardsize);
        addFunction(pthread_attr_setschedparam);
        addFunction(pthread_attr_setschedpolicy);
        addFunction(pthread_attr_setscope);
        addFunction(pthread_attr_setstack);
        addFunction(pthread_attr_setstackaddr);
        addFunction(pthread_attr_setstacksize);
        addFunction(pthread_cond_broadcast);
        addFunction(pthread_cond_destroy);
        addFunction(pthread_cond_init);
        addFunction(pthread_cond_signal);
        addFunction(pthread_cond_timedwait);
        addFunction(pthread_cond_timedwait_monotonic);
        addFunction(pthread_cond_timedwait_monotonic_np);
        addFunction(pthread_cond_timedwait_relative_np);
        addFunction(pthread_cond_timeout_np);
        addFunction(pthread_cond_wait);
        addFunction(pthread_condattr_destroy);
        addFunction(pthread_condattr_init);
        addFunction(pthread_condattr_setpshared);
        addFunction(pthread_create);
        addFunction(pthread_detach);
        addFunction(pthread_equal);
        addFunction(pthread_exit);
        addFunction(pthread_getattr_np);
        addFunction(pthread_getschedparam);
        addFunction(pthread_join);
        addFunction(pthread_key_create);
        addFunction(pthread_key_delete);
        addFunction(pthread_kill);
        addFunction(pthread_mutex_destroy);
        addFunction(pthread_mutex_init);
        addFunction(pthread_mutex_trylock);
        addFunction(pthread_mutexattr_destroy);
        addFunction(pthread_mutexattr_init);
        addFunction(pthread_mutexattr_setpshared);
        addFunction(pthread_mutexattr_settype);
        addFunction(pthread_rwlock_destroy);
        addFunction(pthread_rwlock_init);
        addFunction(pthread_rwlock_rdlock);
        addFunction(pthread_rwlock_unlock);
        addFunction(pthread_rwlock_wrlock);
        addFunction(pthread_self);
        addFunction(pthread_setname_np);
        addFunction(pthread_setschedparam);
        addFunction(pthread_setspecific);
        addFunction(pthread_sigmask);
        addFunction(ptrace);
        addFunction(putc);
        addFunction(putchar);
        addFunction(putenv);
        addFunction(puts);
        addFunction(putwc);
        addFunction(pwrite);
        addFunction(qsort);
        addFunction(raise);
        addFunction(read);
        addFunction(readdir);
        addFunction(readdir_r);
        addFunction(readlink);
        addFunction(readv);
        addFunction(realpath);
        addFunction(recv);
        addFunction(recvfrom);
        addFunction(recvmsg);
        addFunction(regcomp);
        addFunction(regerror);
        addFunction(regexec);
        addFunction(regfree);
        addFunction(remainder);
        addFunction(remainderf);
        addFunction(remove);
        addFunction(remquo);
        addFunction(remquof);
        addFunction(rename);
        addFunction(rewind);
        addFunction(rint);
        addFunction(rintf);
        addFunction(rmdir);
        addFunction(round);
        addFunction(roundf);
        addFunction(sbrk);
        addFunction(scalbn);
        addFunction(scandir);
        addFunction(scanf);
        addFunction(sched_get_priority_max);
        addFunction(sched_get_priority_min);
        addFunction(sched_getparam);
        addFunction(sched_setscheduler);
        addFunction(sched_yield);
        addFunction(select);
        addFunction(sem_close);
        addFunction(sem_destroy);
        addFunction(sem_getvalue);
        addFunction(sem_init);
        addFunction(sem_open);
        addFunction(sem_post);
        addFunction(sem_timedwait);
        addFunction(sem_trywait);
        addFunction(sem_unlink);
        addFunction(sem_wait);
        addFunction(send);
        addFunction(sendfile);
        addFunction(sendmsg);
        addFunction(sendto);
        addFunction(set_sched_policy);
        addFunction(setbuf);
        addFunction(setenv);
        addFunction(setgid);
        addFunction(setitimer);
        addFunction(setjmp);
        addFunction(setpgid);
        addFunction(setpriority);
        addFunction(setresuid);
        addFunction(setrlimit);
        addFunction(setsid);
        addFunction(setsockopt);
        addFunction(settimeofday);
        addFunction(setvbuf);
        addFunction(shutdown);
        addFunction(sigaltstack);
        addFunction(siglongjmp);
        addFunction(sigprocmask);
        addFunction(sigsetjmp);
        addFunction(sigsuspend);
        addFunction(sin);
        addFunction(sinf);
        addFunction(sinh);
        addFunction(sinhf);
        addFunction(sk_free);
        addFunction(sk_pop);
        addFunction(slCreateEngine);
        addFunction(sleep);
        addFunction(socket);
        addFunction(socketpair);
        addFunction(sqlite3_bind_blob);
        addFunction(sqlite3_bind_int);
        addFunction(sqlite3_bind_text);
        addFunction(sqlite3_close);
        addFunction(sqlite3_column_blob);
        addFunction(sqlite3_column_bytes);
        addFunction(sqlite3_column_int);
        addFunction(sqlite3_column_text);
        addFunction(sqlite3_errmsg);
        addFunction(sqlite3_exec);
        addFunction(sqlite3_finalize);
        addFunction(sqlite3_open);
        addFunction(sqlite3_prepare);
        addFunction(sqlite3_step);
        addFunction(sqrt);
        addFunction(sqrtf);
        addFunction(srand48);
        addFunction(sscanf);
        addFunction(stat);
        addFunction(statfs);
        addFunction(strcasestr);
        addFunction(strcat);
        addFunction(strchr);
        addFunction(strcoll);
        addFunction(strcpy16);
        addFunction(strcspn);
        addFunction(strerror);
        addFunction(strftime);
        addFunction(strlcat);
        addFunction(strlcpy);
        addFunction(strlen16);
        addFunction(strlen32);
        addFunction(strncasecmp);
        addFunction(strncat);
        addFunction(strncmp);
        addFunction(strncmp16);
        addFunction(strncpy);
        addFunction(strndup);
        addFunction(strnlen);
        addFunction(strpbrk);
        addFunction(strptime);
        addFunction(strrchr);
        addFunction(strsep);
        addFunction(strsignal);
        addFunction(strspn);
        addFunction(strtod);
        addFunction(strtoimax);
        addFunction(strtok);
        addFunction(strtok_r);
        addFunction(strtol);
        addFunction(strtoll);
        addFunction(strtoul);
        addFunction(strtoull);
        addFunction(strtoumax);
        addFunction(strxfrm);
        addFunction(strzcmp16);
        addFunction(swprintf);
        addFunction(swscanf);
        addFunction(symlink);
        addFunction(sync);
        addFunction(syscall);
        addFunction(sysconf);
        addFunction(syslog);
        addFunction(system);
        addFunction(systemTime);
        addFunction(tan);
        addFunction(tanf);
        addFunction(tanh);
        addFunction(tanhf);
        addFunction(tgamma);
        addFunction(time);
        addFunction(timegm64);
        addFunction(timer_create);
        addFunction(timer_delete);
        addFunction(timer_settime);
        addFunction(times);
        addFunction(tkill);
        addFunction(tmpfile);
        addFunction(tmpnam);
        addFunction(tolower);
        addFunction(toupper);
        addFunction(towlower);
        addFunction(towupper);
        addFunction(trunc);
        addFunction(truncate);
        addFunction(truncf);
        addFunction(tzset);
        addFunction(umask);
        addFunction(uname);
        addFunction(uncompress);
        addFunction(ungetc);
        addFunction(ungetwc);
        addFunction(unlink);
        addFunction(unsetenv);
        addFunction(usleep);
        addFunction(utf16_to_utf8);
        addFunction(utf16_to_utf8_length);
        addFunction(utf32_from_utf8_at);
        addFunction(utf32_to_utf8);
        addFunction(utf32_to_utf8_length);
        addFunction(utf8_to_utf16);
        addFunction(utf8_to_utf16_length);
        addFunction(utf8_to_utf32);
        addFunction(utf8_to_utf32_length);
        addFunction(utime);
        addFunction(utimes);
        addFunction(valloc);
        addFunction(vasprintf);
        addFunction(vfprintf);
        addFunction(vfscanf);
        addFunction(vprintf);
        addFunction(vsnprintf);
        addFunction(vsprintf);
        addFunction(vsscanf);
        addFunction(vswprintf);
        addFunction(vsyslog);
        addFunction(vwprintf);
        addFunction(wait);
        addFunction(waitid);
        addFunction(waitpid);
        addFunction(wcrtomb);
        addFunction(wcscat);
        addFunction(wcschr);
        addFunction(wcscmp);
        addFunction(wcscoll);
        addFunction(wcscpy);
        addFunction(wcsftime);
        addFunction(wcslen);
        addFunction(wcsncat);
        addFunction(wcsncmp);
        addFunction(wcsncpy);
        addFunction(wcsrtombs);
        addFunction(wcsspn);
        addFunction(wcsstr);
        addFunction(wcstol);
        addFunction(wcstombs);
        addFunction(wcsxfrm);
        addFunction(wctob);
        addFunction(wctype);
        addFunction(wmemchr);
        addFunction(wmemcmp);
        addFunction(wmemcpy);
        addFunction(wmemmove);
        addFunction(wmemset);
        addFunction(write);
        addFunction(writev);
        addFunction(wscanf);
        addFunction(xaCreateEngine);
        addFunction(zError);
        addFunction(zlibCompileFlags);
        addFunction(log);
//        functionMap.put("zlibVersion", EMPTY_INT);
//        functionMap.put("__stack_chk_guard", new Immediate(0xe2dee396)); // internal value
//        functionMap.put("_tolower_tab_", Immediate.ZERO); // internal value

        int i=0;
        for(Map.Entry<String, AbstractValue> entry:functionMap.entrySet()) {
            functions.write(i, entry.getValue());
            functionAddressMap.put(entry.getKey(), new Address(functions, i));
            i = i + 4;
        }

//        i=i-4;
//        for(short v:_C_tolower_) {
//            functions.write(i, new Immediate(v), Modifier.TYPE_HALF);
//            i = i +2;
//        }
    }

    public static Address getFunctionAddress(String name) {
        return functionAddressMap.get(name);
    }

    private InternalFunction(String name, Type[] parameterType, Type returnType, Kind kind) {
        super(name, parameterType, returnType, false);
        this.kind = kind;
    }

    private InternalFunction(String name, Type[] parameterType, Type returnType) {
        this(name, parameterType, returnType, Kind.NONE);
    }

    private static void addFunction(InternalFunction function) {
        functionMap.put(function.name, function);
    }

    public static Collection<AbstractValue> getFunctions() {
        return functionMap.values();
    }

    public static AbstractValue get(String name) {
        return functionMap.get(name);
    }

    public boolean isRestricted() {
        return kind == Kind.RESTRICT;
    }

    public boolean isExit() {
        return kind == Kind.EXIT;
    }

    public String toString() {
        return "Internal:" + Misc.ANSI_CYAN + name + Misc.ANSI_RESET;
    }

    public boolean exec(NativeLibraryHandler handler, LibraryModule module, Subroutine subroutine, Stack<Subroutine> depth) throws IOException, Z3Exception {
        logger.debug("Internal Function Call : {}", name);

        if (kind == Kind.SKIP) {
            logger.debug("Skipped");
            return false;
        }

        AbstractValue[] params = new AbstractValue[parameterTypes.length];
        Context ctx = handler.getContext();

        int index = 0;
        for (int i = 0; i < parameterTypes.length; i++) { // prepare parameters; index 0 is JNIEvn all the time
            AbstractValue v = handler.getValueOfParameter(index); // get actual param pattern from regs and stack
            if (parameterTypes[i] == LONG) {
                Long value = Misc.combineToLong(v.intValue(), (handler.getValueOfParameter(++index).intValue()));
                if (value == null) {
                    params[i] = Immediate.newValue(parameterTypes[i]);
                } else {
                    params[i] = new Immediate(value);
                }

//            } else if (parameterTypes[i] == STRING) {
//                params[i] = getStringValue(module, v);
//            } else if (parameterTypes[i] == FORMAT) {
//                params[i] = new StringValue(new Formatter().format(getStringValue(module, v).toString(), handler, module, i + 1).toString());
            } else if (parameterTypes[i] == DOUBLE && (v.isImmediate() && v.getNumeral().getType() != TYPE_DOUBLE)) {
                params[i] = new Immediate(new Numeral(ctx.mkConcat(handler.getValueOfParameter(++index).getNumeral().getBitVecExpr(ctx), v.getNumeral().getBitVecExpr(ctx)).simplify(), TYPE_DOUBLE));
            } else if (parameterTypes[i] == INT) {
                params[i] = v;
                if (params[i].isAssociatedValue()) {
                    params[i] = params[i].getValue();
                }

                if (params[i] == null) {
                    params[i] = new Immediate();
                } else if (!params[i].isImmediate()) {
                    Integer value = params[i].intValue();
                    if (value == null) {
                        params[i] = new Immediate();
                        ;
                    } else {
                        params[i] = new Immediate(value);
                    }
                } else if(params[i].isPseudoValue()) {
                    ((PseudoValue)params[i]).setPossibleType(PseudoValue.PossibleType.INT);
                }
            } else if (parameterTypes[i] == ADDRESS) {
                params[i] = v.getValue();
            } else if (parameterTypes[i] == STRING) {
                params[i] = getStringValue(module, v);
            } else {
                params[i] = v;
            }
            index++;
        }

        // treat special internal function
        if (this == _ZN7android14AndroidRuntime9getJNIEnvEv) {
            Register.setValue(Register.R0, handler.pEnv);
        } else if (this == free || this == _ZdaPv || this == _ZdlPv) {
            if (params[0].isIMemoryValue()) {
                ((IMemoryValue) params[0]).free();
            }
            logger.info("memory deallocated by " + name);
            Register.setValue(Register.R0, Immediate.ZERO);
        } else if (this == calloc) {
            AbstractValue size = MultiplyInstruction.mul(ctx, params[0], params[1]);
            AllocatedMemory data = new AllocatedMemory(size);
            data.memset(Immediate.ZERO, size);
            Register.setValue(Register.R0, data); // it will be assigned to an address or JNIParam
            logger.warn("memory has been allocated by " + getName());
        } else if (this == malloc || this==_Znwj) {
            AllocatedMemory data = new AllocatedMemory(params[0]);
            AbstractValue size = data.size();
            if (size.isUnknown()) {
                if (handler.getValueOfParameter(0).isAssociatedValue()) {
                    ((AssociatedValue) handler.getValueOfParameter(0)).replaceValue(size); //new PseudoValue(handler.getValueOfParameter(0), 0, Immediate.MINUS_ONE));
                }
            }
            Register.setValue(Register.R0, data); // it will be assigned to an address or JNIParam
            logger.warn("memory has been allocated by " + getName());
        } else if (this == realloc) {
            if (params[0].isPseudoValue()) {
                Register.setValue(Register.R0, ((PseudoValue) params[0]).realloc(new Immediate(params[1].getNumeral())));
            } else {
                Register.setValue(Register.R0, ((AllocatedMemory) params[0]).realloc(new Immediate(params[1].getNumeral())));
            }
        } else if (this == memcpy || this == __aeabi_memcpy) {
            if (params[1].isAssociatedValue()) {
                params[1] = ((AssociatedValue) params[1]).getValueAddress();
            }

            if (params[2].isAssociatedValue()) {
                params[2] = params[2].getValue();
            }

            if (params[0].isIMemoryValue()) {
                if (!params[1].isUnknown()) {
                    ((IMemoryValue) params[0]).memcpy((IMemoryValue) params[1], params[2]);
                }
            } else {
                throw new RuntimeException("not implemented");
            }
        } else if (this == memmove || this == __aeabi_memmove) {
            if (params[0].isIMemoryValue()) {
                ((IMemoryValue) params[0]).memmove((IMemoryValue) params[1], params[2]);
            } else {
                throw new RuntimeException("not implemented");
            }
        } else if (this == memcmp) {
            try {
                if (params[2].intValue() == 0) {
                    Register.setValue(Register.R0, Immediate.ZERO);
                } else {
                    throw new RuntimeException("not implemented");
                }
            } catch (Exception e) {
                throw new RuntimeException("not imeplemented");
//                Register.setValue(Register.R0, new Immediate(Modifier.TYPE_INT));
            }

        } else if (this == memset || this == __aeabi_memset) {
            if (params[0].isIMemoryValue()) {
                ((IMemoryValue) params[0]).memset(params[1], params[2]);
            } else {
                throw new RuntimeException("not implemented");
            }
        } else if (this == fseek) {
            if(params[0].isFileValue()) {
                Register.setValue(Register.R0, ((FileValue)params[0]).fseek(params[1], params[2]));
            } else {
                Register.setValue(Register.R0, Immediate.MINUS_ONE);
            }
        } else if(this == ftell) {
            if(params[0].isFileValue()) {
                Register.setValue(Register.R0, ((FileValue)params[0]).ftell());
            } else {
                Register.setValue(Register.R0, Immediate.MINUS_ONE);
            }
        } else if (this == fread) {
            if(params[3].isFileValue()) {
                Register.setValue(Register.R0, ((FileValue)params[3]).fread(ctx, params[0], params[1], params[2]));
            } else {
                Register.setValue(Register.R0, Immediate.MINUS_ONE);
            }
        } else if (this == fclose) {
            Register.setValue(Register.R0, Immediate.ZERO);
        } else if (this == setlocale) {
            StringValue locale = (StringValue) params[1]; // getStringValue(module, params[1]);
            if (!locale.isUnknown()) {
                Register.setValue(Register.R0, new AllocatedMemory(locale));
            } else {
                Register.setValue(Register.R0, new AllocatedMemory("C"));
            }
        } else if (this == getenv) {
            StringValue name = (StringValue) params[0]; // getStringValue(module, params[0]);
            logger.info("getenv - {}", name);
            String result = System.getenv(name.toString());
            if (result != null) {
                Register.setValue(Register.R0, new AllocatedMemory(result));
            } else {
                Register.setValue(Register.R0, Immediate.ZERO); // return null
                logger.warn("unknown environment : {}", name);
            }
        } else if (this == fopen || this == open) {
            StringValue name = (StringValue) params[0]; // getStringValue(module, params[0]);
            logger.info("fopen - {}", name);
            if (name.toString() != null) {
                Register.setValue(Register.R0, new FileValue(new File(name.toString())));
            } else {
                Register.setValue(Register.R0, new FileValue(null)); // new FileValue(null))); // new Immediate());
            }
        } else if (this == read) {
//            Register.setValue(Register.R0, ((FileValue)params[0]).fnew FileValue(new File(name.toString())));
//            if(params[0].isUnknown()) {
//                Register.setValue(Register.R1, )
//            }
            // open file.......
//            ssize_t read(int fildes, void *buf, size_t nbyte);
//            int  open(  char  *filename,  int  access,  int  permission  );
            throw new RuntimeException("open");
        } else if (this == sigaction) {
            Register.setValue(Register.R0, Immediate.ZERO); // success
        } else if (this == pthread_mutex_lock
                || this == pthread_mutex_unlock
                || this == pthread_mutex_destroy
                || this == pthread_cond_init
                || this == pthread_cond_wait
                || this == pthread_mutex_init
                || this == pthread_mutexattr_init
                || this == pthread_mutexattr_destroy
                || this == pthread_mutexattr_settype
                ) {
            Register.setValue(Register.R0, Immediate.ZERO); // success
        } else if (this == getpid || this == gettid) {
            Register.setValue(Register.R0, pid_t); // success
        } else if (this == pthread_key_create) {
            ((IMemoryValue) params[0]).write(Pthread.getKey(), TYPE_INT);
            Register.setValue(Register.R0, Immediate.ZERO); // success
        } else if(this == pthread_setspecific)  {
            if(params[0].isPseudoValue()) {
                // read deep memory block
                AbstractValue v = ((PseudoValue)params[0]).deepRead();
                if(v!=null) {
                    params[0]=v;
                }
            }
            ((Pthread.Key)params[0]).setSpecificValue(params[1]);
            Register.setValue(Register.R0, Immediate.ZERO); // success
        } else if (this == pthread_getspecific) {
            if(params[0].isPseudoValue()) {
                // read deep memory block
                AbstractValue v = ((PseudoValue)params[0]).deepRead();
                if(v!=null) {
                    params[0]=v;
                }
            }
            Register.setValue(Register.R0, ((Pthread.Key)params[0]).getSpecificValue());
        } else if (this == pthread_once) {
            // execute initial function
            Address address = (Address) params[0];

//            Address init =
            // the result of executing the second parameter, will be assigned to the first parameter
            throw new RuntimeException("N/A");
//            ((Address) params[0]).write(new NewValue);
//            Register.setValue(Register.R0, Immediate.ZERO); // success
        } else if (this == pthread_create) {
            ((Address) params[0]).write(new Pthread());
            Register.setValue(Register.R0, Immediate.ZERO); // success
        } else if (this == pthread_detach) {
            if (params[0].isPthread_t()) {
                ((Pthread) params[0]).setDetached();
            } else {
                throw new RuntimeException("unknown address");
            }
            Register.setValue(Register.R0, Immediate.ZERO); // success
        } else if (this == strdup) {
            Register.setValue(Register.R0, new AllocatedMemory(getStringValue(module, params[0])));
        } else if (this == strcat) {
            Register.setValue(Register.R0, new AllocatedMemory(getStringValue(module, params[0]).toString() + getStringValue(module, params[0]).toString()));
        } else if (this == strncat) {
            Register.setValue(Register.R0, new AllocatedMemory((getStringValue(module, params[0]).toString() + getStringValue(module, params[1]).toString()).substring(0, params[1].intValue())));
//        } else if (this == strstr) {
//            if(params[1].isUnknown()) {
//                Register.setValue(Register.R0, params[0]);
//            } else {
//                int pos = -1;
//                String s = getStringValue(module, params[0]).toString();
//                String s1 = getStringValue(module, params[1]).toString();
//                if (s.startsWith("JNI String:")) { // AbstractValue.empty cannot be used here.
//                    pos = s.length()-1;
//                } else {
//                    s.indexOf(s1);
//                }
//                if(pos<0) {
//                    Register.setValue(Register.R0, params[0].getReplacement(params[0].getNumeral().add(s.length())));
//                } else {
//                    Register.setValue(Register.R0, params[0].getReplacement(params[0].getNumeral().add(pos)));
//                }
//            }
        } else if (this == strcmp) {
            if(params[0].isPseudoValue()) {
                ((PseudoValue)params[0]).setPossibleType(PseudoValue.PossibleType.STRING);
            }
            if(params[1].isPseudoValue()) {
                ((PseudoValue)params[1]).setPossibleType(PseudoValue.PossibleType.STRING);
            }

            if(params[0].isPseudoValue() || params[1].isPseudoValue() || params[0].isUnknown() || params[1].isUnknown()) {
                Register.setValue(Register.R0, new Immediate());
                return false;
            }
            Register.setValue(Register.R0, new Immediate(getStringValue(module, params[0]).toString().compareTo(getStringValue(module, params[1]).toString())));
        } else if (this == strncmp) {
            if(params[0].isPseudoValue()) {
                ((PseudoValue)params[0]).setPossibleType(PseudoValue.PossibleType.STRING);
            }
            if(params[1].isPseudoValue()) {
                ((PseudoValue)params[1]).setPossibleType(PseudoValue.PossibleType.STRING);
            }

            if(params[0].isPseudoValue() || params[1].isPseudoValue() || params[0].isUnknown() || params[1].isUnknown()) {
                Register.setValue(Register.R0, new Immediate());
                return false;
            }
            Register.setValue(Register.R0, new Immediate(getStringValue(module, params[0]).toString().substring(0, params[2].intValue()).compareTo(getStringValue(module, params[1]).toString().substring(0, params[2].intValue()))));
        } else if (this == strcasecmp) {
            if(params[0].isPseudoValue()) {
                ((PseudoValue)params[0]).setPossibleType(PseudoValue.PossibleType.STRING);
            }
            if(params[1].isPseudoValue()) {
                ((PseudoValue)params[1]).setPossibleType(PseudoValue.PossibleType.STRING);
            }

            if(params[0].isPseudoValue() || params[1].isPseudoValue() || params[0].isUnknown() || params[1].isUnknown()) {
                Register.setValue(Register.R0, new Immediate());
                return false;
            }
            Register.setValue(Register.R0, new Immediate(getStringValue(module, params[0]).toString().compareToIgnoreCase(getStringValue(module, params[1]).toString())));
            } else if (this == strlen) {
                if(params[0].isPseudoValue()) {
                    ((PseudoValue)params[0]).setPossibleType(PseudoValue.PossibleType.STRING);
                }
                StringValue v = getStringValue(module, params[0]);
                if(v==null || v.length()<0) {
                    Register.setValue(Register.R0, new Immediate(new Numeral().setPositive(ctx)));
                } else {
                    Register.setValue(Register.R0, new Immediate(v.length()));
                }
        } else if (this == strcpy) {
            if(params[0].isPseudoValue()) {
                ((PseudoValue)params[0]).setPossibleType(PseudoValue.PossibleType.STRING);
            }
            if(params[1].isPseudoValue()) {
                ((PseudoValue)params[1]).setPossibleType(PseudoValue.PossibleType.STRING);
            }

            if (params[0].isIMemoryValue()) {
                ((IMemoryValue) params[0]).write(getStringValue(module, params[1]), Modifier.TYPE_BYTE);
            } else {
                throw new RuntimeException("error");
            }
        } else if (this == strchr) {
            StringValue v = getStringValue(module, params[0]);
            String str = v.toString();
            if(str!=null && !params[1].isUnknown()) {
                Integer ch = params[1].intValue();
                for(int i=0;i<str.length();i++) {
                    if(str.charAt(i)==ch) {
                        Register.setValue(Register.R0, v.add(new Immediate(i)));
                        break;
                    }
                }
            } else {
                Register.setValue(Register.R0, new Immediate());
            }
        } else if (this == strncpy) {
            if(!params[0].isUnknown() && params[0].isAddress()) {
                if (params[1].isUnknown()) {
                    ((Address) params[0]).write(params[1], Modifier.TYPE_INT);
                } else {
                    if (!params[2].isUnknown()) {
                        ((Address) params[0]).writeText(getStringValue(module, params[1]).toString().substring(0, params[2].intValue()));
                    } else {
                        ((Address) params[0]).writeText(getStringValue(module, params[1]).toString());
                    }
                }
            } else {
                throw new RuntimeException("error");
            }
        } else if (this == asprintf) {
            String str = new Formatter().format(getStringValue(module, params[1]).toString(), handler, module, 2).toString();
            logger.warn(name+": {}", str.toString());
            ((Address) params[0]).write(new AllocatedMemory(str));
            Register.setValue(Register.R0, new Immediate(str.length()));
        } else if (this == printf) {
            String str = new Formatter().format(getStringValue(module, params[0]).toString(), handler, module, 1).toString();
            logger.warn(name + ": {}", str.toString());
            Register.setValue(Register.R0, new Immediate(str.length()));
        } else if (this == snprintf || this==vsnprintf) {
            String str = new Formatter().format(getStringValue(module, params[2]).toString(), handler, module, 3).toString();
            logger.warn(name + ": {}", str);
            int size = params[1].intValue();
            if (size < str.length()) {
                ((Address) params[0]).writeText(str.toString().substring(0,size));
                Register.setValue(Register.R0, new Immediate(size));
            } else {
                ((Address) params[0]).writeText(str);
                Register.setValue(Register.R0, new Immediate(str.length()));
            }
        } else if (this == sprintf) {
            String str = new Formatter().format(getStringValue(module, params[1]).toString(), handler, module, 2).toString();
            logger.warn(name + ": {}", str);
            ((Address) params[0]).writeText(str);
            Register.setValue(Register.R0, new Immediate(str.length()));
        } else if (this == fprintf) {
            String str = new Formatter().format(getStringValue(module, params[1]).toString(), handler, module, 2).toString();
            logger.warn(name + ": {}", str);
            Register.setValue(Register.R0, new Immediate(str.length()));
        } else if (this == __cxa_atexit || this == __aeabi_atexit) {
            Register.setValue(Register.R0, Immediate.ZERO); // success
        } else if (this == setjmp) {
            ((Address) params[0]).write(new JmpBuf(depth));
            Register.setValue(Register.R0, Immediate.ZERO);
        } else if (this == longjmp) {
            AbstractValue v = ((Address) params[0]).read();
            if (v instanceof JmpBuf) {
                Register.setValue(Register.R0, params[1]);
                ((JmpBuf) v).restore(depth);
            } else {
                throw new RuntimeException("unsupported internal function:" + name);
            }
        } else if(this==sleep) { // context switch
            if (Immediate.ZERO == params[0]) {
                throw new RuntimeException("unsupported internal function:" + name);
//                context swithch..
            }
        } else if(this==__errno) {
            Register.setValue(Register.R0, new AllocatedMemory(Immediate.WORD)); // return address of error no
        } else if(this==mktime) {
            Address addr = ((Address) params[0]);
            Calendar cal = Calendar.getInstance();
            for(int i=0;i<8;i++) {
                AbstractValue v = addr.read(i * 4, Modifier.TYPE_INT);
                if(v!=null) {
                    switch(i) {
                        case 0:
                            cal.set(Calendar.SECOND, v.intValue());
                            break;
                        case 1:
                            cal.set(Calendar.MINUTE, v.intValue());
                            break;
                        case 2:
                            cal.set(Calendar.HOUR_OF_DAY, v.intValue());
                            break;
                        case 3:
                            cal.set(Calendar.DATE, v.intValue());
                            break;
                        case 4:
                            cal.set(Calendar.MONTH, v.intValue());
                            break;
                        case 5:
                            cal.set(Calendar.YEAR, v.intValue()+1900);
                            break;
                        case 6:
                            cal.set(Calendar.DAY_OF_WEEK, v.intValue());
                            break;
                        case 7:
                            cal.set(Calendar.DAY_OF_YEAR, v.intValue()-1);
                            break;
                    }
                }
            }
            Register.setValue(Register.R0, new Immediate(cal.getTimeInMillis() / 1000, TYPE_INT));
        } else if(this==time) {
//            throw new RuntimeException("N/A");
            ((IMemoryValue) params[0]).write(new Immediate(Calendar.getInstance().getTimeInMillis() / 1000, TYPE_INT), TYPE_INT);
        } else if(this==OPENSSL_add_all_algorithms_noconf) {
            // do nothing
        } else if(this==sinf) {
            AbstractValue v;
            if (!params[0].isUnknown() && params[0].isImmediate()) {
                v = new Immediate((float)Math.sin(params[0].getNumeral().floatValue()));
            } else {
                v = Immediate.newValue(TYPE_FLOAT);
                v.getNumeral().setPositive(ctx);
            }
            Register.setValue(Register.R0, v);
        } else if(this==acosf) {
            AbstractValue v;
            if (!params[0].isUnknown() && params[0].isImmediate()) {
                v = new Immediate((float)Math.acos(params[0].getNumeral().floatValue()));
            } else {
                v = Immediate.newValue(TYPE_FLOAT);
                v.getNumeral().setPositive(ctx);
            }
            Register.setValue(Register.R0, v);
        } else if(this==round) { // non fixed value
            Immediate v;
            if (!params[0].isUnknown() && params[0].isImmediate()) {
                v = new Immediate((double)Math.round(params[0].getNumeral().doubleValue()));
            } else {
                v = Immediate.newValue(TYPE_DOUBLE);
                v.getNumeral().setPositive(ctx);
            }
            Register.setValue(Register.R0, v.getLower(ctx));
            Register.setValue(Register.R1, v.getUpper(ctx));
        } else if(this==cos) { // non fixed value
            Immediate v;
            if (!params[0].isUnknown() && params[0].isImmediate()) {
                v = new Immediate(Math.cos(params[0].getNumeral().doubleValue()));
            } else {
                v = Immediate.newValue(TYPE_DOUBLE);
                v.getNumeral().setPositive(ctx);
            }
            Register.setValue(Register.R0, v.getLower(ctx));
            Register.setValue(Register.R1, v.getUpper(ctx));
        } else if(this==log) { // non fixed value
            Immediate v;
            if (!params[0].isUnknown() && params[0].isImmediate()) {
                v = new Immediate(Math.log(params[0].getNumeral().doubleValue()));
            } else {
                v = Immediate.newValue(TYPE_DOUBLE);
                v.getNumeral().setPositive(ctx);
            }
            Register.setValue(Register.R0, v.getLower(ctx));
            Register.setValue(Register.R1, v.getUpper(ctx));
        } else if (this == fwrite) {
            Register.setValue(Register.R0, Immediate.ONE);
        } else if (this == __aeabi_fdiv) {
            Immediate v;
            if(!params[0].isUnknown() && !params[1].isUnknown()) {
                v = new Immediate(params[0].getNumeral().floatValue() / params[1].getNumeral().floatValue());
            } else {
                v = Immediate.newValue(Modifier.TYPE_FLOAT);
            }
            Register.setValue(Register.R0, v);
        } else if (this == __aeabi_dmul) {
            Immediate v;
            if(!params[0].isUnknown() && !params[1].isUnknown()) {
                v = new Immediate(params[0].getNumeral().doubleValue() * params[1].getNumeral().doubleValue());
            } else {
                v = Immediate.newValue(TYPE_DOUBLE);
                v.getNumeral().setPositive(ctx);
            }
            Register.setValue(Register.R0, v.getLower(ctx));
            Register.setValue(Register.R1, v.getUpper(ctx));
        } else if (this == pow) {
            Immediate v;
            if(!params[0].isUnknown() && !params[1].isUnknown()) {
                v = new Immediate(Math.pow(params[0].getNumeral().doubleValue(), params[1].getNumeral().doubleValue()));
            } else {
                v = Immediate.newValue(TYPE_DOUBLE);
                v.getNumeral().setPositive(ctx);
            }
            Register.setValue(Register.R0, v.getLower(ctx));
            Register.setValue(Register.R1, v.getUpper(ctx));
        } else if (this == __aeabi_d2f) {
            if (!params[0].isUnknown()) {
                Register.setValue(Register.R0, new Immediate(params[0].getNumeral().doubleValue().floatValue()));
            } else {
                Register.setValue(Register.R0, Immediate.newValue(Modifier.TYPE_FLOAT));
            }

  /*      } else if(this==log) { // non fixed value
            if (!params[0].isUnknown() && params[0].isImmediate()) {
                int[] values = Misc.separateToInt(Math.log(((Immediate) params[0]).doubleValue()));
                Register.setValue(Register.R0, new Immediate(values[0]));
                Register.setValue(Register.R1, new Immediate(values[1]));
            } else {
                Immediate v = Immediate.newValue(TYPE_DOUBLE);
                Register.setValue(Register.R0, v.getLower());
                v.getUpper().getNumeral().addExpr(new SingleExpr(Expr.SIGN.GE, 0));
                Register.setValue(Register.R1, v.getUpper());
            }
        } else if(this==round) { // non fixed value
            if (!params[0].isUnknown() && params[0].isImmediate()) {
                int[] values = Misc.separateToInt(Math.round(((Immediate) params[0]).doubleValue()));
                Register.setValue(Register.R0, new Immediate(values[0]));
                Register.setValue(Register.R1, new Immediate(values[1]));
            } else {
                Immediate v = Immediate.newValue(TYPE_DOUBLE);
                Register.setValue(Register.R0, v.getLower());
                Register.setValue(Register.R1, v.getUpper());
            }
        } else if(this==cos) { // non fixed value
            if (!params[0].isUnknown() && params[0].isImmediate()) {
                int[] values = Misc.separateToInt(Math.cos(((Immediate) params[0]).doubleValue()));
                Register.setValue(Register.R0, new Immediate(values[0]));
                Register.setValue(Register.R1, new Immediate(values[1]));
            } else {
                Immediate v = Immediate.newValue(TYPE_DOUBLE);
                Register.setValue(Register.R0, v.getLower());
                Register.setValue(Register.R1, v.getUpper());
            }
        } else if (this == __aeabi_d2f) {
            if (!params[0].isUnknown() && params[0].isImmediate()) {
                Register.setValue(Register.R0, new Immediate(((Immediate)params[0]).doubleValue().floatValue()));
            } else {
                Register.setValue(Register.R0, new Immediate());
            }
        } else if (this == __aeabi_fdiv) {
            if (!params[0].isUnknown() && !params[1].isUnknown() && params[0].isImmediate() && params[1].isImmediate()) {
                Register.setValue(Register.R0, new Immediate(((Immediate) params[0]).floatValue() / ((Immediate) params[1]).floatValue()));
            } else {
                Register.setValue(Register.R0, new Immediate());
            }
*/
        } else if(this == __android_log_print || this == __aeabi_idiv) {
// do nothing
        } else if (this == dlopen) {
            Register.setValue(Register.R0, Immediate.ZERO); // return value first
            StringValue fileName = getStringValue(module, params[0]);
            String libName;
            logger.info("dlopen for {} ignored", fileName);
            if (fileName != null && fileName.toString().endsWith(".so")) {
                libName = fileName.toString();
                int pos = libName.lastIndexOf("/");
                libName = libName.substring(pos + 4, libName.length() - 3);
                if (handler.getModule(libName) != null) {
                    Register.setValue(Register.R0, new StringValue(libName));
                } else {
                    throw new RuntimeException("dlerror "+ libName);
                    //handler.putErrorMsg("dlerror", libName);
                }
            }
        } else if (this == dlerror) {
            String errorMsg = handler.getErrorMsg(name);
            if (errorMsg != null) {
                Register.setValue(Register.R0, new AllocatedMemory(errorMsg));
            } else {
                Register.setValue(Register.R0, new AllocatedMemory("dlopen_error"));
            }
        } else if (this == glReadPixels) {
            ((IMemoryValue)params[6]).write(Immediate.newValue(Modifier.TYPE_INT), TYPE_INT);
        } else if ( this == EVP_aes_256_cbc) {

        } else if ( this == glTexCoordPointer
                || this == glHint
                || this == glTexImage2D
                || this == glGenTextures
                || this == glClearDepthf
                || this == glRotatef
                || this == glEnableClientState
                || this == glDrawArrays
                || this == glDisable
                || this == glMatrixMode
                || this == glViewport
                || this == glTexParameteri
                || this == glLoadMatrixf
                || this == glLightfv
                || this == glLoadIdentity
                || this == glPopMatrix
                || this == glVertexPointer
                || this == glClearColor
                || this == glFrontFace
                || this == glDepthFunc
                || this == glMaterialf
                || this == glPushMatrix
                || this == glBindTexture
                || this == glEnable
                || this == glClear
                || this == glMaterialfv
                || this == glShadeModel
                || this == glScalef
                || this == glTranslatef
                || this == glColor4f
                || this == glBlendFunc
                || this == glGenBuffers
                || this == glCreateShader
                || this == glCreateProgram
                || this == glGenerateMipmap
                || this == glFrustumf) {
            // restircted..
        } else if (this == dlsym) {
            StringValue libName = getStringValue(module, params[0]);
            StringValue symbolName = getStringValue(module, params[1]);
            logger.info("dlsym for {} of {} ignored", symbolName, libName);
            SubroutineOrFunction symbol = handler.findSymbol(symbolName.toString(), libName.toString());
            Register.setValue(Register.R0, symbol);
        } else if (this==lrand48) {
            Immediate v = Immediate.newValue(TYPE_DOUBLE);
            Register.setValue(Register.R0, v.getLower(ctx));
            Register.setValue(Register.R1, v.getUpper(ctx));
        } else if (this==mmap) {
            Register.setValue(Register.R0, new AllocatedMemory(params[1]));
        } else if (this==stat || this==writev) {
            Register.setValue(Register.R0, new Immediate());
        } else if(this==exit) {
          return true;
        } else if(this==btowc || this==wctob) {
          // nothing to change
        } else if(this==wctype) {
            StringValue property = getStringValue(module, params[0]);
            if (property != null && !property.isUnknown()) {
                String str = property.toString();
                if (str != null) {
                    for (int i = 0; i < wctype_property.length; i++) {
                        if (wctype_property[i].equals(str)) {
                            Register.setValue(Register.R0, new Immediate(i));
                            return false;
                        }
                    }
                }
            }
            Register.setValue(Register.R1, new Immediate());
        } else if(this == putchar) {
        } else if(this == close || this==wait) {

        } else if (this==abort ||  this == __stack_chk_fail) {
            return true;  // exit!
        } else if(this == AndroidBitmap_getInfo || this==AndroidBitmap_lockPixels || this==AndroidBitmap_unlockPixels) {
//            int AndroidBitmap_getInfo	(	JNIEnv * 	env,
//                    jobject 	jbitmap,
//                    AndroidBitmapInfo * 	info
//            )
//            uint32_t 	width
//            uint32_t 	height
//            uint32_t 	stride
//            int32_t 	format
//            uint32_t 	flags
            //params[2];

            Register.setValue(Register.R0, ANDROID_BITMAP_RESULT_SUCCESS);
        } else {
            throw new RuntimeException("unsupported internal function:" + name);

        }
        return isExit();
    }

    private static String[] wctype_property = { "<invalid>", "alnum", "alpha", "blank", "cntrl", "digit", "graph", "lower", "print", "punct", "space", "upper", "xdigit"};

    private enum Kind {EXIT, RESTRICT, NONE, SKIP}
}
