package org.langzhaozhi.nmshortpath;

import java.util.Arrays;

import org.langzhaozhi.common.graph.Vertex;

/**
 * NM-最短路径图顶点,简称顶点
 */
public final class NMShortPathVertex<A> implements Vertex<A> {
    final A mAttachment;//此顶点绑定的数据对象,具体由应用作出解释
    final NMShortPathGraph<A> mOwnerGraph;
    final int mGraphVertexIndex;
    NMShortPathEdge<A> [] mPreviousEdges;

    /**
     * 创建一个只有一个前向边的顶点
     * @param aAttachment 此顶点附带数据
     * @param aPreviousVertex 前向顶点
     * @param aDistanceFromPreviousVertex 前向顶点到此顶点的距离
     */
    @SuppressWarnings("unchecked")
    public NMShortPathVertex(A aAttachment, NMShortPathVertex<A> aPreviousVertex, double aDistanceFromPreviousVertex) {
        this( aAttachment, new NMShortPathEdge<A>( aPreviousVertex, aDistanceFromPreviousVertex ) );
    }

    /**
     * 创建一个有若干条前向边的顶点，至少有一条前向边
     * @param aAttachment 此顶点附带数据
     * @param aPreviousEdges 连到此顶点的前向边，至少有一条
     */
    public NMShortPathVertex(A aAttachment, @SuppressWarnings("unchecked") NMShortPathEdge<A>... aPreviousEdges) {
        if (aPreviousEdges.length == 0) {
            //至少传递一条前向边
            throw new IllegalArgumentException( "AtLeast Pass One Previous Edge With One Previous Vertex" );
        }
        this.mAttachment = aAttachment;
        this.mOwnerGraph = aPreviousEdges[ 0 ].mPreviousVertex.mOwnerGraph;
        this.mPreviousEdges = aPreviousEdges;
        this.mGraphVertexIndex = this.mOwnerGraph.generateVertexIndex();

        NMShortPathVertex<A> endVertex = this.mOwnerGraph.getEndVertex();
        for (int i = 0; i < aPreviousEdges.length; ++i) {
            //前向顶点不能是endVertex,父图不能是其他图
            if (aPreviousEdges[ i ].mPreviousVertex == endVertex || aPreviousEdges[ i ].mPreviousVertex.mOwnerGraph != this.mOwnerGraph) {
                throw new IllegalArgumentException( "Previous Edge[" + i + "] not valid" );
            }
        }
        this.mOwnerGraph.addNewCreatedVertex( this );
        //初始对多个前向边按照相邻距离排下序,一般可以稍微加快后面NMShortPath计算过程
        if (this.mPreviousEdges.length > 1) {
            Arrays.sort( this.mPreviousEdges, (aOne, aTwo) -> aOne.mDistanceBetweenVertexes < aTwo.mDistanceBetweenVertexes ? -1 : aOne.mDistanceBetweenVertexes == aTwo.mDistanceBetweenVertexes ? 0 : 1 );
        }
    }

    public NMShortPathGraph<A> getOwnerContext() {
        return this.mOwnerGraph;
    }

    @Override
    public A getAttachment() {
        return this.mAttachment;
    }

    /**
     * 令本顶点连接到终止顶点，等价于调用 theVertex.getOwnerContext().connectToEndVertex( theVertex, distanceToEndVertex );
     * @param aDistanceToEndVertex
     */
    public void connectToEndVertex(double aDistanceToEndVertex) {
        this.mOwnerGraph.connectToEndVertex( this, aDistanceToEndVertex );
    }

    /**
     * 内部用途，只用于构造起始顶点和终止顶点 ,外部不能用此方法创建顶点
     */
    NMShortPathVertex(A aAttachment, NMShortPathGraph<A> aOwnerContext, int aGraphVertexIndex) {
        this.mAttachment = aAttachment;
        this.mOwnerGraph = aOwnerContext;
        this.mPreviousEdges = null;
        this.mGraphVertexIndex = aGraphVertexIndex;
    }
}
