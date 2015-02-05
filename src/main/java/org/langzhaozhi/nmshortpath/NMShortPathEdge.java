package org.langzhaozhi.nmshortpath;

import org.langzhaozhi.common.graph.Edge;

/**
 * NM-最短路径图的边,一个顶点邻接的边，用于在创建NMShortPathVertex<A>顶点的时候传递相邻边的信息。
 * 正如Edge中的说明所述,此边的"this"顶点相对于此Edge定义而言是隐含明确定义的,在NM-最短路径中用于创建一个
 * "this"顶点时，同时明确指定与其关联的所有前向的边。"other"顶点在这里实际就是此前向边的另一端前向顶点
 *
 * @see Edge
 */
public final class NMShortPathEdge<A> implements Edge<A, NMShortPathVertex<A>> {
    final NMShortPathVertex<A> mPreviousVertex;//前向顶点
    final double mDistanceBetweenVertexes;//从前向顶点到"this"顶点的两点距离

    public NMShortPathEdge(NMShortPathVertex<A> aPreviousVertex, double aDistanceBetweenVertexes) {
        if (aPreviousVertex == null) {
            throw new IllegalArgumentException( "The other vertext can not be null" );
        }
        this.mPreviousVertex = aPreviousVertex;
        //对两点距离不做任何限制如必须大于0之类，由应用者自己根据应用情况决定是否允许负值
        this.mDistanceBetweenVertexes = aDistanceBetweenVertexes;
    }

    @Override
    public NMShortPathVertex<A> getOtherVertex() {//这里的"other"顶点被解释为前向顶点
        return this.getPreviousVertex();
    }

    @Override
    public double getWeight() {//这里的权重被解释成两顶点之间的距离
        return this.getDistance();
    }

    public NMShortPathVertex<A> getPreviousVertex() {
        return this.mPreviousVertex;
    }

    /**
     * 此边的距离为两顶点之间的距离，即前向顶点到后向顶点的距离
     * @return 距离数值
     */
    public double getDistance() {
        return this.mDistanceBetweenVertexes;
    }
}
