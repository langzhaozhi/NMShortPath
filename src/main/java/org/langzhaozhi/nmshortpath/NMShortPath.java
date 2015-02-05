package org.langzhaozhi.nmshortpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * NMShortPath 最终的NM-最短路径的结果，包含了1到最多N个(可能小于N值)从起始顶点到终止顶点具有不同累计距离的ShortPath；
 * 而每个ShortPath实际可能1条到最多M条不同的顶点路径序列，这些顶点路径序列具有相同的从起始顶点到终止顶点的累计距离。
 */
public final class NMShortPath<A> {
    private final ShortPath<A> [] mShortPathArray;

    NMShortPath(ShortPath<A> [] aShortPathArray) {
        this.mShortPathArray = aShortPathArray;
    }

    /**
     * 获取NM-最短路径的ShortPah实际条数，小于等于N
     * @return 实际ShortPath个数
     */
    public int getShortPathCount() {
        return this.mShortPathArray.length;
    }

    /**
     * 获取NM-最短路径的VertexPath不同顶点路径序列的个数，小于等于M
     * @return 实际VertexPath条数
     */
    public int getVertexPathCount() {
        return Arrays.stream( this.mShortPathArray ).mapToInt( aShortPath -> aShortPath.mSameTotalDistanceVertexPathArray.length ).sum();
    }

    /**
     * 获取某条NM-最短路径,由aIndex制定
     * @param aIndex
     * @return
     */
    public ShortPath<A> getShortPathAt(int aIndex) {
        return this.mShortPathArray[ aIndex ];
    }

    /**
     * 获取NM-最短路径中最最短的那条，实际就是计算结果的第0个ShortPath
     * @return 最最短路径ShortPath
     */
    public ShortPath<A> getMostShortPath() {
        return this.mShortPathArray[ 0 ];
    }

    public void forEach(Consumer<ShortPath<A>> aAction) {
        Arrays.stream( this.mShortPathArray ).forEach( aAction );
    }

    /**
     * 从起始顶点到当前顶点的 i-短路径，每个ShortPath 可能包含一条或多条OnePath, 它们具有相同的累计距离值
     */
    public static final class ShortPath<A> {
        final double mTotalDistance;//从起始顶点到当前顶点的累计距离

        //相同于此累计距离的各条具体的不同顶点的顶点路径
        final VertexPath<A> [] mSameTotalDistanceVertexPathArray;

        ShortPath(double aTotalDistance, VertexPath<A> [] aSameTotalDistanceVertexPathArray) {
            this.mTotalDistance = aTotalDistance;
            this.mSameTotalDistanceVertexPathArray = aSameTotalDistanceVertexPathArray;
        }

        public double getTotalDistance() {
            return this.mTotalDistance;
        }

        public int getVertexPathCount() {
            return this.mSameTotalDistanceVertexPathArray.length;
        }

        public VertexPath<A> getVertexPathAt(int aIndex) {
            return this.mSameTotalDistanceVertexPathArray[ aIndex ];
        }

        public void forEach(Consumer<VertexPath<A>> aAction) {
            Arrays.stream( this.mSameTotalDistanceVertexPathArray ).forEach( aAction );
        }
    }

    /**
     * 从起始顶点到终止顶点的的某一路径序列,主要记录一条具体路径的顶点序列,累计的路径距离在ShortPath中
     */
    public static final class VertexPath<A> {
        final NMShortPathVertex<A> mCurrentVertex;//当前顶点的ID或下标索引

        final VertexPath<A> mPreviousVertexPath;//前向顶点的对应VertexPath

        final double mDistanceFromPreviousVertex;//从前向顶点到当前顶点的距离(相邻两顶点距离)

        ArrayList<VertexPath<A>> mCacheFromStartToEndVertexList = null;

        VertexPath(NMShortPathVertex<A> aStartVertex) {//起始顶点的VertexPath特殊处理下
            this.mCurrentVertex = aStartVertex;
            this.mPreviousVertexPath = this;
            this.mDistanceFromPreviousVertex = 0;
        }

        VertexPath(NMShortPathVertex<A> aCurrentVertex, VertexPath<A> aPreviousVertexPath, double aDistanceFromPreviousVertex) {
            this.mCurrentVertex = aCurrentVertex;
            this.mPreviousVertexPath = aPreviousVertexPath;
            this.mDistanceFromPreviousVertex = aDistanceFromPreviousVertex;
        }

        /**
         * 获取此顶点路径序列的顶点个数
         * @return
         */
        public int getVertexCount() {
            return this.ensureCache().size();
        }

        /**
         * 获取第 aIndex 个顶点
         * @param aIndex 此顶点路径序列的第 aIndex 个顶点的下标(从0开始)
         * @return 顶点
         */
        public NMShortPathVertex<A> getVertexAt(int aIndex) {
            return this.ensureCache().get( aIndex ).mCurrentVertex;
        }

        /**
         * 获取此路径顶点序列中任意两顶点之间的距离，可以相邻的两顶点，也可以中间有其他间隔顶点
         * @param aFromVertexIndex 此路径顶点序列中的from顶点
         * @param aToVertexIndex 此路径顶点序列中的to顶点
         * @return 两顶点距离
         */
        public double getDistanceBetween(int aFromVertexIndex, int aToVertexIndex) {
            aFromVertexIndex = aFromVertexIndex < 0 ? 0 : aFromVertexIndex;
            aToVertexIndex = aToVertexIndex >= this.getVertexCount() ? this.getVertexCount() - 1 : aToVertexIndex;
            ArrayList<VertexPath<A>> segments = this.ensureCache();
            double distanceBetween = 0;
            for (int i = aToVertexIndex; i > aFromVertexIndex; --i) {
                distanceBetween += segments.get( i ).mDistanceFromPreviousVertex;
            }
            return distanceBetween;
        }

        /**
         * 从起始顶点到终止顶点循环
         * @param aAction
         */
        public void forEach(Consumer<NMShortPathVertex<A>> aAction) {
            this.ensureCache().forEach( (aPathSegment) -> aAction.accept( aPathSegment.mCurrentVertex ) );
        }

        /**
         * 从终止顶点到起始顶点的循环
         */
        public void forEachVertexReverse(Consumer<NMShortPathVertex<A>> aAction) {
            VertexPath<A> currentPathSegment = this;
            while (true) {
                aAction.accept( currentPathSegment.mCurrentVertex );
                if (currentPathSegment.mCurrentVertex.mGraphVertexIndex == 0) {
                    break;
                }
                else {
                    currentPathSegment = currentPathSegment.mPreviousVertexPath;
                }
            }
        }

        private ArrayList<VertexPath<A>> ensureCache() {
            ArrayList<VertexPath<A>> cacheList = this.mCacheFromStartToEndVertexList;
            if (cacheList == null) {
                LinkedList<VertexPath<A>> fromStartToEndVertexPath = new LinkedList<VertexPath<A>>();
                VertexPath<A> currentPathSegment = this;
                while (true) {
                    fromStartToEndVertexPath.addFirst( currentPathSegment );
                    if (currentPathSegment.mCurrentVertex.mGraphVertexIndex == 0) {
                        break;
                    }
                    else {
                        currentPathSegment = currentPathSegment.mPreviousVertexPath;
                    }
                }
                cacheList = new ArrayList<VertexPath<A>>( fromStartToEndVertexPath );
                this.mCacheFromStartToEndVertexList = cacheList;
            }
            return cacheList;
        }
    }
}
