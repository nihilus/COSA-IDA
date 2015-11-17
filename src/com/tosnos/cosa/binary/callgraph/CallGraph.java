package com.tosnos.cosa.binary.callgraph;

import com.tosnos.cosa.binary.function.Subroutine;

import java.util.*;

/**
 * Created by kevin on 12/14/14.
 */
public class CallGraph {
    private Set<Edge> edges = new HashSet<Edge>();
    private Map<Subroutine, List<Edge>> srcMethodToEdges = new HashMap<Subroutine, List<Edge>>();

    public boolean addEdge(Edge e) {
        if (!edges.add(e)) return false;
        Subroutine src = e.src();
        List<Edge> edges = srcMethodToEdges.get(src);
        if (edges == null) {
            edges = new ArrayList<Edge>();
            srcMethodToEdges.put(src, edges);
        }
        edges.add(e);

//        position = srcUnitToEdge.get( e.srcUnit() );
//        if( position == null ) {
//            srcUnitToEdge.put( e.srcUnit(), e );
//            position = dummy;
//        }
//        e.insertAfterByUnit( position );
//
//        position = srcMethodToEdge.get( e.getSrc() );
//        if( position == null ) {
//            srcMethodToEdge.put( e.getSrc(), e );
//            position = dummy;
//        }
//
//        e.insertAfterBySrc( position );
//
//        position = tgtToEdge.get( e.getTgt() );
//        if( position == null ) {
//            tgtToEdge.put( e.getTgt(), e );
//            position = dummy;
//        }
//        e.insertAfterByTgt( position );
        return true;
    }


    public List<Edge> edgesOutOf(Subroutine src) {
        return srcMethodToEdges.get(src);
    }

//    class TargetsOfUnitIterator implements Iterator<Edge> {
//        private Edge position = null;
//        private FunctionCall u;
//        TargetsOfUnitIterator( FunctionCall u ) {
//            this.u = u;
//            if( u == null ) throw new RuntimeException();
//            position = srcUnitToEdge.get( u );
//            if( position == null ) position = dummy;
//        }
//        public boolean hasNext() {
//            if( position.srcUnit() != u ) return false;
//            if( position.kind() == Edge.Kind.INVALID ) return false;
//            return true;
//        }
//        public Edge next() {
//            Edge ret = position;
//            position = position.nextByUnit();
//            return ret;
//        }
//        public void remove() {
//            throw new UnsupportedOperationException();
//        }
//    }
}
