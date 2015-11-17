package com.tosnos.cosa.binary.function;

import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.JNI.JNIValue;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;

/**
 * Created by kevin on 7/7/15.
 */
public class NativeMethod extends Subroutine {
    public enum ValueType{ STRING, CLASS, FIELD}

    private SootMethod method;
    private Map<Object, Local> localValueMap = new HashMap<Object, Local>();
    private Stmt retStmt = null;
    private int numOfLocals = 0;

    public NativeMethod(LibraryModule module, String name, int address, SootMethod sootMethod) {
        super(module, name, address);
        this.method = sootMethod;
    }

    public SootMethod getSootMethod() {
        return method;
    }

    public boolean isNativeMethod() {
        return true;
    }

    public JimpleBody getBody() {
        if(!method.hasActiveBody()) {
            method.setModifiers(method.getModifiers() & ~Modifier.NATIVE);
            JimpleBody body = Jimple.v().newBody(method);
            method.setActiveBody(body);
            Chain locals = body.getLocals();
            Chain units = body.getUnits();
            if (!method.isStatic())
            {
                Local thisLocal = Jimple.v().newLocal("$r"+(numOfLocals++), RefType.v(method.getDeclaringClass()));
                locals.add(thisLocal);
                units.addFirst(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef((RefType) thisLocal.getType())));
            }

            Iterator<Type> parIt = method.getParameterTypes().iterator();
            int params = 0;
            while (parIt.hasNext())
            {
                Type type = parIt.next();
                Local local = Jimple.v().newLocal("$r"+(numOfLocals++), type);
                locals.add(local);
                units.addFirst(Jimple.v().newIdentityStmt(local, Jimple.v().newParameterRef(local.getType(), params++)));
            }

            if(method.getReturnType() instanceof VoidType) {
                retStmt = soot.jimple.Jimple.v().newReturnVoidStmt();
                body.getUnits().add(retStmt);
            }
         }
        return (JimpleBody)method.getActiveBody();
    }

    public void addUnit(Unit u) {
        Body body = getBody();
        if(retStmt!=null) {
            body.getUnits().insertBefore(u, retStmt);
        } else {
            body.getUnits().add(u);
        }
    }
    /*
    Value v = null;
                if (localValue != null) {
                    v = localValue.get(paramType);
                }
                if (v == null) {
                    v = Jimple.v().newLocal("$r" + (b.getLocalCount() + 1), paramType);
                    b.getLocals().addFirst((Local) v);

                    Value rvalue;
                    if (paramType instanceof RefType) {
                        rvalue = Jimple.v().newNewExpr((RefType) paramType);
                    } else if (paramType instanceof ArrayType) {
                        if (((ArrayType) paramType).numDimensions == 1) {
                            rvalue = Jimple.v().newNewArrayExpr(((ArrayType) paramType).baseType, IntConstant.v(0));
                        } else {
                            List<Value> dims = new ArrayList<Value>();
                            for (int num = 0; num < ((ArrayType) paramType).numDimensions; num++) {
                                dims.add(IntConstant.v(0));
                            }
                            rvalue = Jimple.v().newNewMultiArrayExpr((ArrayType) paramType, dims);
                        }
                    } else {
                        throw new RuntimeException(method.getParameterType(i).toString());
                    }

                    Unit u = Jimple.v().newAssignStmt(v, rvalue);


                    b.getUnits().insertAfter(u, lastInsertedUnit);
                    lastInsertedUnit = u;
                    if (localValue != null) {
                        localValue.put(method.getParameterType(i), v);
                    }
                }
                invoke.setArg(i, v);
//                args.set(i, v);

            Chain units = body.getUnits();
        Chain locals = body.getLocals();

        ParameterRef paramRef = Jimple.v().newParameterRef(ArrayType.v(RefType.v("java.lang.String"), 1), 0);
        Local paramLocal = Jimple.v().newLocal("$r0", ArrayType.v(RefType.v("java.lang.String"), 1));
        locals.add(paramLocal);
        units.add(Jimple.v().newIdentityStmt(paramLocal, paramRef));

        for (Component component : manifest.getComponents()) {
            SootMethod method = component.getEntryPoint(this);
            if (method != null && !sc.declaresMethod(method.getNumberedSubSignature())) {
                sc.addMethod(method);
                setReceiver(method);
                units.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(method.makeRef())));

            }
        }

        units.add(Jimple.v().newReturnVoidStmt());
     */
    public Local addLocal(AbstractValue value, Type paramType) {
        Value v = localValueMap.get(paramType);

        if (v == null) {
            Body b = getBody();
            v = Jimple.v().newLocal("$r" + (numOfLocals++), paramType);
            b.getLocals().addFirst((Local) v);

            Value rvalue;
            if (paramType instanceof RefType) {
                rvalue = Jimple.v().newNewExpr((RefType) paramType);
            } else if (paramType instanceof ArrayType) {
                if (((ArrayType) paramType).numDimensions == 1) {
                    rvalue = Jimple.v().newNewArrayExpr(((ArrayType) paramType).baseType, IntConstant.v(0));
                } else {
                    List<Value> dims = new ArrayList<Value>();
                    for (int num = 0; num < ((ArrayType) paramType).numDimensions; num++) {
                        dims.add(IntConstant.v(0));
                    }
                    rvalue = Jimple.v().newNewMultiArrayExpr((ArrayType) paramType, dims);
                }
            } else {
          //      throw new RuntimeException(method.getParameterType(i).toString());
            }

        //    Unit u = Jimple.v().newAssignStmt(v, rvalue);


//            b.getUnits().insertAfter(u, lastInsertedUnit);
//            lastInsertedUnit = u;
//            if (localValue != null) {
//                localValue.put(method.getParameterType(i), v);
//            }
        }
//
//        Local local = localMap.get(value);
//        if(local==null) {
//            JimpleBody body = getBody();
//            Chain units = body.getUnits();
//            Chain locals = body.getLocals();
//            ParameterRef paramRef = null;
//            Local paramLocal = null;
//            if(type == ValueType.STRING) {
//                paramRef = Jimple.v().newParameterRef(ArrayType.v(RefType.v("java.lang.String"), 1), 0);
//                paramLocal = Jimple.v().newLocal("$r0", ArrayType.v(RefType.v("java.lang.String"), 1));
//                locals.add(paramLocal);
//                units.add(Jimple.v().newIdentityStmt(paramLocal, paramRef));
//            }
//            locals.add(paramLocal);
//            units.add(Jimple.v().newIdentityStmt(paramLocal, paramRef));
//        }
        return (Local) v;
    }

    public Local[] getLocal(NativeLibraryHandler handler, AbstractValue[] params, Function function) throws Z3Exception {
        Local[] values = new Local[function.getParameterCount()-1];
        for(int i=0;i<params.length-1;i++) { // the first parameter is ENV
            AbstractValue value = params[i + 1].getValue();
            values[i] = localValueMap.get(value);
            if(values[i]==null) {
                if(params[i+1].isJNIValue() && ((JNIValue)params[i+1]).getParamIndex()>=JNIValue.CLASS) {
                    int paramIndex = ((JNIValue)params[i+1]).getParamIndex();
                    if(paramIndex==JNIValue.THIS) {
                        values[i] = getBody().getThisLocal();
                        localValueMap.put(value, values[i]);
                    } else if(paramIndex>=0) {
                        values[i] = getBody().getParameterLocal(paramIndex);
                        localValueMap.put(value, values[i]);
                    }
                } else {
                    Type paramType = function.getParamType(i+1);
                    if(paramType==Function.VA_LIST) {

                    } else {

                    }
                }
            }
        }
        return values;
    }

    public Local getLocal(ConcreteRef ref) {
        Local value = localValueMap.get(ref);
        if(value==null) {
            value = Jimple.v().newLocal("$r"+(numOfLocals++), ref.getType());
            getBody().getLocals().add(value);
            localValueMap.put(ref, value);
        }
        return value;
    }



}
