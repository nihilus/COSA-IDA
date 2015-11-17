package com.tosnos.cosa.binary.callgraph;

import com.tosnos.cosa.binary.function.Subroutine;

/**
 * Created by kevin on 12/14/14.
 */
public class Edge {
    private final Subroutine src, tgt;
    private final Kind kind;

    public Edge(Subroutine src, Subroutine tgt) {
        this(src, tgt, Kind.Direct);
    }

    public Edge(Subroutine src, Subroutine tgt, Kind kind) {
        assert (src != null && tgt != null);
        this.src = src;
        this.tgt = tgt;
        this.kind = kind;
    }

    public Subroutine src() {
        return src;
    }

    public Subroutine tgt() {
        return tgt;
    }

    public Kind kind() {
        return kind;
    }

    public enum Kind {Direct, Indirect, Internal, INVALID}
}
