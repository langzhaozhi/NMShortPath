package org.langzhaozhi.nmshortpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.langzhaozhi.common.graph.GraphStrictOnewayWithStartEndVerteies;
import org.langzhaozhi.nmshortpath.NMShortPath.ShortPath;
import org.langzhaozhi.nmshortpath.NMShortPath.VertexPath;

/**
 * NM-最短路径图，(N <= M)，带明确定义起点终点的严格单向图，对每个顶点存在至少一条通向终点的路径。
 * 当N==1时退化成经典的单源最短路径图
 *
 * @param <A> 绑定于图顶点上的数据对象,具体由应用作出解释,不做任何限制性规定
 * @see README.md 中有关N和M的说明
 */
public final class NMShortPathGraph<A> implements GraphStrictOnewayWithStartEndVerteies<A, NMShortPathVertex<A>> {
    private static final int END_VERTEX_INDEX = 0xFFFFFFFF;
    private final NMShortPathVertex<A> mStartVertex;
    private final NMShortPathVertex<A> mEndVertex;
    private final int mNShortPathCount;//N值
    private final int mMVertexPathCount;//M值
    private int mVertexIndexGenerator = 0;

    //各顶点包括startVertex但不包括endVertex，如果一个顶点暂时没有后向顶点，就在其index上设置为null
    private ArrayList<NMShortPathVertex<A>> mVertexesWithPostVertexes = new ArrayList<NMShortPathVertex<A>>();
    boolean mReady;

    public NMShortPathGraph() {
        this( 1, 1 );
    }

    public NMShortPathGraph(int aNShortPathCount, int aMVertexPathCount) {
        this( aNShortPathCount, aMVertexPathCount, null, null );
    }

    public NMShortPathGraph(int aNShortPathCount, int aMVertexPathCount, A aStartAttachment, A aEndAttachment) {
        if (aNShortPathCount <= 0 || aMVertexPathCount < aNShortPathCount) {//N > 0 && N <= M
            throw new IllegalArgumentException( "N(" + aNShortPathCount + ") or M(" + aMVertexPathCount + ") not valid" );
        }
        this.mStartVertex = new NMShortPathVertex<A>( aStartAttachment, this, this.generateVertexIndex() );//startVertex永远是0号
        this.mEndVertex = new NMShortPathVertex<A>( aEndAttachment, this, NMShortPathGraph.END_VERTEX_INDEX );
        this.mNShortPathCount = aNShortPathCount;
        this.mMVertexPathCount = aMVertexPathCount;
        this.mVertexesWithPostVertexes.add( null );//mStartVertex 对应的占位
    }

    @Override
    public NMShortPathVertex<A> getStartVertex() {
        return this.mStartVertex;
    }

    @Override
    public NMShortPathVertex<A> getEndVertex() {
        return this.mEndVertex;
    }

    //N值
    public int getNShortPathCount() {
        return this.mNShortPathCount;
    }

    //M值
    public int getMVertexPathCount() {
        return this.mMVertexPathCount;
    }

    public int getGraphVertexCount() {
        //+1 表示endVertex，因为endVertex 的 graphVertexIndex 比较特殊 0xFFFFFFFF
        return this.mVertexIndexGenerator + 1;
    }

    public void connectToEndVertex(NMShortPathVertex<A> aPreviousVertex, double aDistanceToEndVertex) {
        if (aPreviousVertex.mOwnerGraph != this) {
            throw new IllegalArgumentException();
        }
        NMShortPathVertex<A> endVertex = this.mEndVertex;
        if (endVertex.mPreviousEdges == null) {
            @SuppressWarnings("unchecked")
            NMShortPathEdge<A> [] edges = new NMShortPathEdge [] {
                new NMShortPathEdge<A>( aPreviousVertex, aDistanceToEndVertex )
            };
            endVertex.mPreviousEdges = edges;
        }
        else {
            int oldPreviousVertexCount = endVertex.mPreviousEdges.length;
            endVertex.mPreviousEdges = Arrays.copyOf( endVertex.mPreviousEdges, oldPreviousVertexCount + 1 );
            endVertex.mPreviousEdges[ oldPreviousVertexCount ] = new NMShortPathEdge<A>( aPreviousVertex, aDistanceToEndVertex );
        }
        //此 aPreviousVertex 顶点此时有自己的前向顶点和后向顶点了
        this.mVertexesWithPostVertexes.set( aPreviousVertex.mGraphVertexIndex, aPreviousVertex );
    }

    public boolean isReady() {
        if (!this.mReady) {
            //除了endVertex顶点外，所有其他顶点都存在至少一个后向顶点
            this.mReady = this.mVertexesWithPostVertexes.stream().allMatch( (aNext) -> aNext != null );
        }
        return this.mReady;
    }

    public NMShortPath<A> calculateNMShortPath() {
        if (!this.isReady()) {
            //图模型还未准备就绪：存在没有后向顶点的顶点
            throw new IllegalStateException( "The NShortPathGraph Model Is NOT Ready: There Are Invalid Vertex Which Has No Post Vertexes" );
        }

        ArrayList<NMShortPathVertex<A>> vertexes = this.mVertexesWithPostVertexes;
        int nshortPathCount = this.mNShortPathCount;
        int mvertexPathCount = this.mMVertexPathCount;

        //每个顶点都有从起始顶点到它的最多N条(可能小于N) ShortPath,由下标对应，如0对应起始顶点本身的
        ArrayList<ShortPath<A> []> shortPathsOfVertexes = new ArrayList<ShortPath<A> []>( vertexes.size() + 1 );
        @SuppressWarnings("unchecked")
        ShortPath<A> [] startPathArray = new ShortPath [ 1 ];
        @SuppressWarnings("unchecked")
        VertexPath<A> [] startVertexPathArray = new VertexPath [] {
            new VertexPath<A>( this.mStartVertex )
        };
        startPathArray[ 0 ] = new ShortPath<A>( 0.0, startVertexPathArray );//起始顶点特殊处理下其前向累计段，累计距离初始成0
        shortPathsOfVertexes.add( startPathArray );

        ArrayList<ShortPath<A>> cacheShortPathArray = new ArrayList<ShortPath<A>>( nshortPathCount );//cache use: 当前顶点的至多N条ShortPath
        ArrayList<VertexPath<A>> cacheVertexPathArray = new ArrayList<VertexPath<A>>( nshortPathCount );
        ArrayList<PreviousEdgeShortPathGroup<A>> cachePreviousEdgeShortPathGroupList = new ArrayList<PreviousEdgeShortPathGroup<A>>( nshortPathCount );//cache use: 当前顶点前向边最短路径组
        ArrayList<PreviousEdgeShortPathGroup<A>> cacheInsertGroupList = new ArrayList<PreviousEdgeShortPathGroup<A>>( nshortPathCount );//cache use: 当前顶点前向边最短路径组,用于下一轮排序插入
        GroupCache<A> groupCache = new GroupCache<A>( nshortPathCount );//小优化：避免大量 PreviousEdgeShortPathGroup 临时用途的垃圾产生，重复利用实例

        for (int i = 1, ilast = vertexes.size(); i <= ilast; ++i) {
            NMShortPathVertex<A> currentVertex = i < ilast ? vertexes.get( i ) : this.mEndVertex;//当前顶点,i==ilast表示endVertex
            NMShortPathEdge<A> [] previousEdgeArray = currentVertex.mPreviousEdges;
            int previousEdgeCount = previousEdgeArray.length;

            if (previousEdgeCount == 1) {
                //只有一条前向边，直接累加,此前向顶点已经排序过了，一步直接合并即可，而且必定同时满足N的限制和M的限制
                NMShortPathVertex<A> previousVertex = previousEdgeArray[ 0 ].mPreviousVertex;//此前向边对应的前向顶点
                double distanceBetweenVertexes = previousEdgeArray[ 0 ].mDistanceBetweenVertexes;//两顶点相邻距离
                ShortPath<A> [] previousShortPathArray = shortPathsOfVertexes.get( previousVertex.mGraphVertexIndex );

                @SuppressWarnings("unchecked")
                ShortPath<A> [] currentShortPaths = new ShortPath [ previousShortPathArray.length ];
                for (int j = 0, jsize = previousShortPathArray.length; j < jsize; ++j) {
                    ShortPath<A> previousShortPath = previousShortPathArray[ j ];
                    VertexPath<A> [] previousVertexPathArray = previousShortPath.mSameTotalDistanceVertexPathArray;
                    int vertextPathCount = previousVertexPathArray.length;
                    double thisTotalDistance = previousShortPath.mTotalDistance + distanceBetweenVertexes;
                    @SuppressWarnings("unchecked")
                    VertexPath<A> [] currentVertexSameTotalDistanceVertextPathArray = new VertexPath [ vertextPathCount ];
                    for (int k = 0; k < vertextPathCount; ++k) {
                        currentVertexSameTotalDistanceVertextPathArray[ k ] = new VertexPath<A>( currentVertex, previousVertexPathArray[ k ], distanceBetweenVertexes );
                    }
                    ShortPath<A> thisShortPath = new ShortPath<A>( thisTotalDistance, currentVertexSameTotalDistanceVertextPathArray );
                    currentShortPaths[ j ] = thisShortPath;
                }
                shortPathsOfVertexes.add( i, currentShortPaths );
            }
            else {
                cacheShortPathArray.clear();//clear ready for currentVertex use
                cachePreviousEdgeShortPathGroupList.clear();//clear ready for currentVertex use
                groupCache.reset();//reset ready for currentVertex use
                ArrayList<ShortPath<A>> currentShortPathArray = cacheShortPathArray;
                List<PreviousEdgeShortPathGroup<A>> previousEdgeShortPathGroupList = cachePreviousEdgeShortPathGroupList;

                //核心算法：对当前顶点的各前向边进行分组成<前向边的最短路径组>，每个前向顶点之前已经计算好并排序好了其自身的NM-最短距离，
                //对当前顶点只需要从各个前向组中依次取出排好序的第一个来比较即可
                for (int j = 0; j < previousEdgeCount; ++j) {
                    NMShortPathEdge<A> previousEdge = previousEdgeArray[ j ];
                    NMShortPathVertex<A> previousVertex = previousEdge.mPreviousVertex;//此前向边对应的前向顶点
                    ShortPath<A> [] previousShortPathArray = shortPathsOfVertexes.get( previousVertex.mGraphVertexIndex );
                    previousEdgeShortPathGroupList.add( groupCache.fromCache( previousEdge, previousShortPathArray ) );
                }
                //初始组数目同前向边数目相同
                int groupCount = previousEdgeCount;
                //多个不同的前向顶点，虽然每个前向顶点自身已经排过序了，但这些前向顶点到本顶点的累积最短距离还需要再次排序看哪些更短距离
                previousEdgeShortPathGroupList.sort( NMShortPathGraph.mGroupComparator );
                //每个顶点最多N条ShortPath并且最多M条不同顶点路径组合数目(下面的循环分别用n和m表示N的迭代限制和M的迭代限制,注意应该是 m < mlast 而非 m <= mlast 条件,没有++m)
                for (int n = 0, nlast = nshortPathCount - 1, m = 0, mlast = mvertexPathCount - 1; n <= nlast && m < mlast && groupCount > 0; ++n) {
                    //每一轮直接从0开始依次找前面若干个相同的 minDistance 合并即可: previousEdgeShortPathGroupList已经排序好了的
                    double minDistance = previousEdgeShortPathGroupList.get( 0 ).mCurrentTotalDistance;
                    cacheVertexPathArray.clear();//clear ready for current ShortPath use
                    ArrayList<VertexPath<A>> thisVertexPathArray = cacheVertexPathArray;
                    int minDistanceGroupCount = groupCount;
                    cacheInsertGroupList.clear();//clear ready for currentVertex use
                    ArrayList<PreviousEdgeShortPathGroup<A>> currentInsertGroupList = cacheInsertGroupList;
                    for (int k = 0; k < groupCount; ++k) {
                        //从0开始合并直到第一个不是 minDistance 最短距离的
                        PreviousEdgeShortPathGroup<A> nextMinGroup = previousEdgeShortPathGroupList.get( k );
                        if (nextMinGroup.mCurrentTotalDistance == minDistance) {
                            //匹配 minDistance 了的嘛，合并之
                            double distanceBetweenVertexes = nextMinGroup.mPreviousEdge.mDistanceBetweenVertexes;//两相邻顶点距离
                            ShortPath<A> nextFirst = nextMinGroup.getCurrentFirstShortPath();
                            VertexPath<A> [] nextFirstVertexPathArray = nextFirst.mSameTotalDistanceVertexPathArray;
                            for (int l = 0, lsize = nextFirstVertexPathArray.length; l < lsize && m <= mlast; ++l, ++m) {
                                thisVertexPathArray.add( new VertexPath<A>( currentVertex, nextFirstVertexPathArray[ l ], distanceBetweenVertexes ) );
                            }
                            if (nextMinGroup.changeToNextCurrentFirstShortPath()) {//游标下移为下一轮的第一个ShortPath准备
                                //说明还有其他ShortPath,先记录下来,后面用于排序插入
                                currentInsertGroupList.add( nextMinGroup );
                            }
                            else {
                                //说明此group所有ShortPath都是NM-最短路径的前段了，消耗完毕了，移除之
                            }
                        }
                        else {
                            //前面 minDistanceGroupCount 个组到本顶点都是最短距离为 minDistance 的N最短路径之一
                            minDistanceGroupCount = k;
                            break;
                        }
                    }
                    @SuppressWarnings("unchecked")
                    VertexPath<A> [] thisVertexPaths = thisVertexPathArray.toArray( new VertexPath [ thisVertexPathArray.size() ] );
                    ShortPath<A> thisShortPath = new ShortPath<A>( minDistance, thisVertexPaths );
                    //记录下当前顶点的这个ShortPath
                    currentShortPathArray.add( thisShortPath );
                    if (n < nlast && m < mlast) {//小优化: 如果 n==nlast 表示已经达到N条最短路径了，如果 m == mlast 表示已经达到M条不同顶点路径组合数目了，那么最后剩余的部分也没有必要继续排序归并了
                        //关键点1：(Zero-Copy技术,ArrayList::subList代替 remove,彻底消除 remove 的拷贝过程)
                        //关键点2: (这 minDistanceGroupCount 后面遗留的那些 group 已经是排好序的：核心在于要充分利用这个已经排序好的结果嘛)
                        previousEdgeShortPathGroupList = previousEdgeShortPathGroupList.subList( minDistanceGroupCount, groupCount );
                        int needInsertSize = currentInsertGroupList.size();
                        if (needInsertSize > 0) {
                            //关键点3：只把之前记录的没有消耗完的 currentInsertGroupList 排下序：它们已经的状态已经变迁了，需要单独排序比较
                            currentInsertGroupList.sort( NMShortPathGraph.mGroupComparator );
                            //关键点4：最后按排序结果合并即可：扫描一遍依次插入到排序位置即可，本质就是两个已经排好序的集合的归并：算法简单直接做即可
                            for (int k = 0, nextInsertPosition = 0, remainingSize = groupCount - minDistanceGroupCount; k < needInsertSize; ++k) {
                                PreviousEdgeShortPathGroup<A> nextInsertGroup = currentInsertGroupList.get( k );
                                double currentInsertFirstTotalDistance = nextInsertGroup.mCurrentTotalDistance;
                                for (; nextInsertPosition < remainingSize; ++nextInsertPosition) {
                                    PreviousEdgeShortPathGroup<A> nextRemainingGroup = previousEdgeShortPathGroupList.get( nextInsertPosition );
                                    if (nextRemainingGroup.mCurrentTotalDistance >= currentInsertFirstTotalDistance) {
                                        //找到插入位置了。假如果一直找不到，就说明 nextInsertGroup 就应该排序在最后，因此在最末尾插入即可
                                        break;
                                    }
                                }
                                //在 nextInsertPosition 位置上插入：可能在中间插入，也可能在末尾插入
                                previousEdgeShortPathGroupList.add( nextInsertPosition, nextInsertGroup );
                                ++remainingSize;
                                ++nextInsertPosition;
                            }
                        }
                        //到此 previousEdgeShortPathGroupList 又变成排序好的了，下一轮即从0开始依次找前面若干个相同的 minDistance 即可
                        //可能前面有移除的，因此得重新看下其当前group个数还有剩余否
                        groupCount = previousEdgeShortPathGroupList.size();
                    }
                }
                @SuppressWarnings("unchecked")
                ShortPath<A> [] currentShortPaths = currentShortPathArray.toArray( new ShortPath [ currentShortPathArray.size() ] );
                shortPathsOfVertexes.add( i, currentShortPaths );
            }
        }
        //最终结果的NMShortPath 就是终止顶点的至多N条ShortPath并且至多M条VertexPath
        ShortPath<A> [] endVertexShortPathArray = shortPathsOfVertexes.get( shortPathsOfVertexes.size() - 1 );
        NMShortPath<A> finalNShortPath = new NMShortPath<A>( endVertexShortPathArray );
        return finalNShortPath;
    }

    int generateVertexIndex() {
        return this.mVertexIndexGenerator++;
    }

    /**
     * 内部调用,从 NMShortPathVertex 构造函数而来
     * @param aNewVertex 刚创建的顶点
     */
    void addNewCreatedVertex(NMShortPathVertex<A> aNewVertex) {
        ArrayList<NMShortPathVertex<A>> vertexes = this.mVertexesWithPostVertexes;
        //此新创建的顶点尚未有 "to" 顶点，还未成为完备顶点，因此在其所在位置设置null占位
        vertexes.add( aNewVertex.mGraphVertexIndex, null );
        for (NMShortPathEdge<A> previousEdge : aNewVertex.mPreviousEdges) {
            //此 previousVertex 此刻已经有自己的 "from" 顶点和自己的 "to" 顶点了，即使不再连接到其他顶点也已经完备
            vertexes.set( previousEdge.mPreviousVertex.mGraphVertexIndex, previousEdge.mPreviousVertex );
        }
        this.mReady = false;
    }

    //<前向边的最短路径组>
    private static final class PreviousEdgeShortPathGroup<A> {
        //前向边
        NMShortPathEdge<A> mPreviousEdge;

        //已经按照最短路径排好序的ShortPath
        ShortPath<A> [] mPreviousShortPathArray;

        //此前向边最短路径组的当前第一个最短路径到本"this"顶点的累积距离
        double mCurrentTotalDistance;

        //mPreviousShortPathArray 对应的下标
        int mCurrentFirstPreviousShortPathIndex;

        //重置：重复临时实例利用
        PreviousEdgeShortPathGroup<A> reset(NMShortPathEdge<A> aPreviousEdge, ShortPath<A> [] aPreviousShortPathArray) {
            this.mPreviousEdge = aPreviousEdge;
            this.mPreviousShortPathArray = aPreviousShortPathArray;
            this.mCurrentTotalDistance = aPreviousShortPathArray[ 0 ].mTotalDistance + aPreviousEdge.mDistanceBetweenVertexes;
            this.mCurrentFirstPreviousShortPathIndex = 0;
            return this;
        }

        //此前向边最短路径组的当前第一个最短路径
        ShortPath<A> getCurrentFirstShortPath() {
            return this.mPreviousShortPathArray[ this.mCurrentFirstPreviousShortPathIndex ];
        }

        public boolean changeToNextCurrentFirstShortPath() {
            //移动到下一个作为本Group的当前最短累积距离
            if (++this.mCurrentFirstPreviousShortPathIndex == this.mPreviousShortPathArray.length) {
                return false;//完毕,本group所有ShortPath 都是当前"this"顶点的前NMShortPath路径段
            }
            else {
                //游标下移后把到"this"顶点的最短距离累加起来以便下一轮进行比较
                this.mCurrentTotalDistance = this.mPreviousShortPathArray[ this.mCurrentFirstPreviousShortPathIndex ].mTotalDistance + this.mPreviousEdge.mDistanceBetweenVertexes;
                return true;//continue
            }
        }
    }

    //由于PreviousEdgeShortPathGroup实例本身就是临时用途的, 这些对象实例理所应当被重复利用, 避免每个顶点都临时创建大量的用完就丢的垃圾: 顶点越多,N越大那么垃圾数目就越大。
    //测试表明当内存越接近使用极限,那么垃圾碎片的影响越大(可能GC消耗)，因此本优化还是有必要。当然，规模小的时候效果不明显
    private static final class GroupCache<A> {
        private ArrayList<PreviousEdgeShortPathGroup<A>> mCacheList;//当前空闲未用的
        private ArrayList<PreviousEdgeShortPathGroup<A>> mUsedList;//当前正在用的

        GroupCache(int aCacheInitialSize) {
            this.mCacheList = new ArrayList<PreviousEdgeShortPathGroup<A>>( aCacheInitialSize );
            this.mUsedList = new ArrayList<PreviousEdgeShortPathGroup<A>>( aCacheInitialSize );
        }

        void reset() {
            ArrayList<PreviousEdgeShortPathGroup<A>> cacheList = this.mCacheList;
            ArrayList<PreviousEdgeShortPathGroup<A>> usedList = this.mUsedList;
            for (int i = usedList.size() - 1; i >= 0; --i) {//代替Collection::addAll()因为其内部又要new 出一个新的 Object []数组,将抵消本Cache的优化
                cacheList.add( usedList.remove( i ) );
            }
        }

        PreviousEdgeShortPathGroup<A> fromCache(NMShortPathEdge<A> aPreviousEdge, ShortPath<A> [] aPreviousShortPathArray) {
            //cache中没有的话就 new 出个新实例
            ArrayList<PreviousEdgeShortPathGroup<A>> cacheList = this.mCacheList;
            int lastIndex = cacheList.size() - 1;//Array结构一定要从末端开始remove,绝对不能冲0开始，否则剩余元素往前挪的开销将抵消本cache的用途，而且还要得不偿失
            PreviousEdgeShortPathGroup<A> reusedOne = lastIndex >= 0 ? cacheList.remove( lastIndex ) : new PreviousEdgeShortPathGroup<A>();
            //记录到正在用的 list 中
            this.mUsedList.add( reusedOne );
            return reusedOne.reset( aPreviousEdge, aPreviousShortPathArray );

            //可通过把上面注释掉来对比测试看
            //return new PreviousEdgeShortPathGroup<A>().reset( aPreviousEdge, aPreviousShortPathArray );
        }
    }

    //对分组按最短距离的排序定义
    private static Comparator<PreviousEdgeShortPathGroup<?>> mGroupComparator = (aOne, aTwo) -> aOne.mCurrentTotalDistance < aTwo.mCurrentTotalDistance ? -1 : aOne.mCurrentTotalDistance == aTwo.mCurrentTotalDistance ? 0 : 1;
}
