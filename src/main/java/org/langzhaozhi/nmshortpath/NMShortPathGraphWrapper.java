package org.langzhaozhi.nmshortpath;

import java.util.ArrayList;

/**
 * 提供一个简便的NM-最短路径图的创建包装器，有时直接用NShortPathGraph来创建顶点不方便，
 * 因为需要预先确定每个顶点所有的前向边和前向顶点。此包装器提供先创建出所有的顶点，
 * 然后再建立这些顶点之间的边的关系。
 */
public final class NMShortPathGraphWrapper<A> {
    private NMShortPathGraph<A> mGraphModel;

    private A [] mVertexAttachment;
    private InternalVertex [] mInternalVertexes;
    private boolean mReady;

    /**
     * 构造函数预先创建出各个顶点，各顶点从起始顶点到终止顶点依次由 aVertexAttachment 对应，
     * aVertexAttachment[0] 对应起始顶点的，aVertexAttachment[aVertexAttachment.length -1]对应终止顶点的
     *
     * @param aShortPathCount NM-最短路径的N值
     * @param aVertexAttachment 从起始顶点到终止顶点的各个顶点的 Attachment
     */
    public NMShortPathGraphWrapper(int aShortPathCount, int aVertextPathCount, A [] aVertexAttachment) {
        if (aVertexAttachment.length <= 2) {
            throw new IllegalArgumentException( "At Least more than 2 vertexes!" );//至少存在3个顶点嘛
        }
        this.mVertexAttachment = aVertexAttachment;
        InternalVertex [] internalVertexes = new InternalVertex [ aVertexAttachment.length ];
        for (int i = 0; i < internalVertexes.length; ++i) {
            internalVertexes[ i ] = new InternalVertex();
        }
        //对startVertex和endVertex特殊初始化下
        internalVertexes[ 0 ].mPreviousEdge.add( new InternalPostEdge( 0, 0 ) );
        internalVertexes[ internalVertexes.length - 1 ].mHasPostVertex = true;
        this.mInternalVertexes = internalVertexes;

        this.mGraphModel = new NMShortPathGraph<A>( aShortPathCount, aVertextPathCount, aVertexAttachment[ 0 ], aVertexAttachment[ aVertexAttachment.length - 1 ] );
    }

    /**
     * 两顶点之间创建一条边, 边的距离由 aDistanceBetween 指定
     * @param aPreviousGraphVertexIndex
     * @param aPostGraphVertexIndex
     * @param aDistanceBetween
     * @return
     */
    public NMShortPathGraphWrapper<A> createEdge(int aPreviousGraphVertexIndex, int aPostGraphVertexIndex, double aDistanceBetween) {
        if (aPreviousGraphVertexIndex < 0 || aPostGraphVertexIndex >= this.mInternalVertexes.length || aPreviousGraphVertexIndex >= aPostGraphVertexIndex) {
            throw new IllegalArgumentException( "aPreviousGraphVertexIndex=[" + aPreviousGraphVertexIndex + "] or aPostGraphVertexIndex(" + aPostGraphVertexIndex + ") not valid" );
        }
        this.mInternalVertexes[ aPreviousGraphVertexIndex ].mHasPostVertex = true;//此previous顶点已经至少有一个后向顶点了
        this.mInternalVertexes[ aPostGraphVertexIndex ].mPreviousEdge.add( new InternalPostEdge( aPreviousGraphVertexIndex, aDistanceBetween ) );
        //重新建立边
        return this;
    }

    /**
     * 是否所有的顶点已经关联了至少一个前向边并且关联了一条后向边, 返回true表示已经准备好图模型，
     * 可以开始计算NM-最短路径了
     * @return true 表示所有顶点已经准备好
     */
    public boolean isReady() {
        if (!this.mReady) {
            this.mReady = true;
            //除了endVertex顶点外，所有其他顶点都存在至少一个后向顶点
            InternalVertex [] internalVertexes = this.mInternalVertexes;
            int endVertexID = internalVertexes.length - 1;
            for (int i = 0; i <= endVertexID; ++i) {
                InternalVertex nextVertex = internalVertexes[ i ];
                if (!nextVertex.mHasPostVertex || nextVertex.mPreviousEdge.size() == 0) {//注意，已经对startVertex和endVertex特殊初始化过了
                    this.mReady = false;
                    break;
                }
            }
        }
        return this.mReady;
    }

    public NMShortPath<A> calculateNShortPath() {
        if (!this.isReady()) {
            //图模型还未准备就绪：存在没有后向顶点的顶点或没有前向顶点的顶点
            throw new IllegalStateException( "The NShortPathGraph Model Is NOT Ready: There Are Invalid Vertex Which Has No Post Vertexes Or Previous Vertexes" );
        }
        NMShortPathGraph<A> graphModel = this.mGraphModel;
        A [] vertexAttachments = this.mVertexAttachment;
        InternalVertex [] internalVertexes = this.mInternalVertexes;

        @SuppressWarnings("unchecked")
        NMShortPathVertex<A> [] vertexArray = new NMShortPathVertex [ this.mVertexAttachment.length ];
        vertexArray[ 0 ] = graphModel.getStartVertex();
        vertexArray[ vertexArray.length - 1 ] = graphModel.getEndVertex();
        for (int i = 1, ilast = internalVertexes.length - 2; i <= ilast; ++i) {//除开startVertex和endVertex
            InternalVertex nextInternalVertex = internalVertexes[ i ];
            @SuppressWarnings("unchecked")
            NMShortPathEdge<A> [] previousEdges = nextInternalVertex.mPreviousEdge.stream().map( (aPreviousInternalEdge) -> {
                NMShortPathVertex<A> previousVertex = vertexArray[ aPreviousInternalEdge.mPreviousVertexIndex ];//必定非null,因为是依次顺序创建的
                return new NMShortPathEdge<A>( previousVertex, aPreviousInternalEdge.mPreviousEdgeDistance );
            } ).toArray( NMShortPathEdge []::new );

            vertexArray[ i ] = new NMShortPathVertex<A>( vertexAttachments[ i ], previousEdges );
        }
        //对endVertex的各个前向顶点建立同endVertex的边
        internalVertexes[ internalVertexes.length - 1 ].mPreviousEdge.stream().forEach( (aPreviousInternalEdge) -> {
            NMShortPathVertex<A> previousVertex = vertexArray[ aPreviousInternalEdge.mPreviousVertexIndex ];
            previousVertex.connectToEndVertex( aPreviousInternalEdge.mPreviousEdgeDistance );
        } );
        //OK,图模型完备了，可以计算了
        return graphModel.calculateNMShortPath();
    }

    private static final class InternalVertex {
        ArrayList<InternalPostEdge> mPreviousEdge = new ArrayList<InternalPostEdge>();
        boolean mHasPostVertex = false;//是否有后向顶点
    }

    private static final class InternalPostEdge {
        int mPreviousVertexIndex;
        double mPreviousEdgeDistance;

        InternalPostEdge(int aPreviousVertexIndex, double aPreviousEdgeDistance) {
            this.mPreviousVertexIndex = aPreviousVertexIndex;
            this.mPreviousEdgeDistance = aPreviousEdgeDistance;
        }
    }
}
