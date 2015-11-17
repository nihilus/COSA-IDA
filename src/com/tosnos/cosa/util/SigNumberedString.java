package com.tosnos.cosa.util;

import soot.Scene;
import soot.util.NumberedString;

/**
 * Created by kevin on 11/25/14.
 */
public class SigNumberedString {
    public static SigNumberedString instance = null;
    public final NumberedString sigExecute;
    public final NumberedString sigExecutorExecute;
    public final NumberedString sigRun;
    public final NumberedString sigObjRun;
    public final NumberedString sigDoInBackground;
    public final NumberedString sigStart;
    public final NumberedString[] fragmentSigs;
    public final NumberedString sigLoadLibrary;


    private SigNumberedString() {
        this.sigExecute = Scene.v().getSubSigNumberer().findOrAdd("android.os.AsyncTask execute(java.lang.Object[])");
        this.sigExecutorExecute = Scene.v().getSubSigNumberer().findOrAdd("void execute(java.lang.Runnable)");
        this.sigRun = Scene.v().getSubSigNumberer().findOrAdd("void run()");
        this.sigObjRun = Scene.v().getSubSigNumberer().findOrAdd("java.lang.Object run()");
        this.sigDoInBackground = Scene.v().getSubSigNumberer().findOrAdd("java.lang.Object doInBackground(java.lang.Object[])");
        this.sigStart = Scene.v().getSubSigNumberer().findOrAdd("void start()");
        this.fragmentSigs = new NumberedString[]{
                Scene.v().getSubSigNumberer().findOrAdd("void onAttach(android.app.Activity)"),
                Scene.v().getSubSigNumberer().findOrAdd("void onCreate(android.os.Bundle)"),
                Scene.v().getSubSigNumberer().findOrAdd("android.view.View onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)"),
                Scene.v().getSubSigNumberer().findOrAdd("void onActivityCreated(android.os.Bundle)"),
                Scene.v().getSubSigNumberer().findOrAdd("void onStart()"),
                Scene.v().getSubSigNumberer().findOrAdd("void onResume()"),
                Scene.v().getSubSigNumberer().findOrAdd("void onPause()"),
                Scene.v().getSubSigNumberer().findOrAdd("void onStop()"),
                Scene.v().getSubSigNumberer().findOrAdd("void onDestroyView()"),
                Scene.v().getSubSigNumberer().findOrAdd("void onDestroy()"),
                Scene.v().getSubSigNumberer().findOrAdd("void onDetach()")
        };
        this.sigLoadLibrary = Scene.v().getSubSigNumberer().findOrAdd("void loadLibrary(java.lang.String)");

    }

    public synchronized static SigNumberedString v() {
        if (instance == null) {
            instance = new SigNumberedString();
        }
        return instance;
    }

    public synchronized static void reset() {
        instance = null;
    }
}
