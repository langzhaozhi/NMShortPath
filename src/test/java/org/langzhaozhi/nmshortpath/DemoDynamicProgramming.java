package org.langzhaozhi.nmshortpath;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>演示构建一个大规模的动态规划问题的求解：有 ColumnCount 列，每列顶点有 VertexCountOfEveryColumn 个，相邻两列顶点之间是全边连接。</p>
 * <p><b>对内存的增长分析</b><br/>
 * 可惜本人的PC内存不够，只能降低问题规模。影响内存使用最大的因素是每排的顶点个数(内存指数增长),例如 ColumnCount==10000 列,
 * VertexCountOfEveryColumn == 1000个顶点的话，边的条数大约是 1000^2 * 10000 = 100亿条，内存肯定撑爆；而列数对内存增长只是线性的。
 * 而N的值在求解过程中对内存增长也是指数影响的，每个顶点都要保存N求解路径来供后面回溯。运行这个Demo应该在64位机器下，并把JVM虚拟机内存参数调大。
 * 为了防止内存爆炸，特别是不同的顶点路径组合数目的爆炸, 需要通过调节M来限制之。参见 README.md 有关N和M 的说明。</p>
 * <p><b>对求解时间的影响分析</b><br/>
 * N的影响最大，时间复杂度呈现超指数增长。在给定N的情况下，列数和每列顶点数对求解时间反而是线性的。尤其N==1的常规动态规划问题，
 * 更是几乎等价于一次简单遍历(伴随每个顶点的是一次小排序)就立即求解出来。
 * </p>
 *
 * <p>效果还好，算法本身已经到单线程求解的极限，要更快的话只能以后考虑实现并发求解N-最短路径的算法</p>
 */
public class DemoDynamicProgramming {
    private static final int ColumnCount = 500;
    private static final int VertexCountOfEveryColumn = 100;
    private static final int N = 10;//NM-最短路径的N可以依次调 N=1,2,3.....看下情况;
    private static final int M = 10000000;//当M调节的愈来愈大的时候，可以明显看到组合爆炸的问题：表现为内存占用和求解时间的爆炸伤

    public static void main(String [] args) {//运行本程序需要64下预先把JVM调大，如 -Xms4g 或 32位机器下至少 -Xms1g
        AtomicInteger attachmentGenerator = new AtomicInteger( 0 );

        //DebugDistanceGenerator gen = DemoDynamicProgramming.mFixedGenerator;//所有边的距离相同：看组合爆炸,必须由M来压制
        //DebugDistanceGenerator gen = DemoDynamicProgramming.mVertexSameGenerator;//任意两顶点的前向距离相同，但一个顶点的各前向边不同
        //DebugDistanceGenerator gen = DemoDynamicProgramming.mColumnSameGenerator;
        DebugDistanceGenerator gen = DemoDynamicProgramming.mAllNotSameGenerator;//固定N后 M影响趋向于0

        NMShortPathGraph<Integer> graphModel = new NMShortPathGraph<Integer>( N, M, attachmentGenerator.getAndIncrement(), 0xFFFF_FFFF );

        //初始<#起点#>为前一排的顶点
        ArrayList<NMShortPathVertex<Integer>> previousColumnVertexArray = new ArrayList<NMShortPathVertex<Integer>>( DemoDynamicProgramming.VertexCountOfEveryColumn );
        previousColumnVertexArray.add( graphModel.getStartVertex() );

        ArrayList<NMShortPathVertex<Integer>> currentColumnVertexArray = new ArrayList<NMShortPathVertex<Integer>>( DemoDynamicProgramming.VertexCountOfEveryColumn );

        for (int i = 0; i < DemoDynamicProgramming.ColumnCount; ++i) {
            for (int j = 0; j < DemoDynamicProgramming.VertexCountOfEveryColumn; ++j) {
                @SuppressWarnings("unchecked")
                NMShortPathEdge<Integer> [] previousEdgeArray = previousColumnVertexArray.stream().map( (aPreviousVertex) -> new NMShortPathEdge<Integer>( aPreviousVertex, gen.generateDistance() ) ).toArray( NMShortPathEdge []::new );
                NMShortPathVertex<Integer> currentVertex = new NMShortPathVertex<Integer>( attachmentGenerator.getAndIncrement(), previousEdgeArray );
                currentColumnVertexArray.add( currentVertex );
            }

            ArrayList<NMShortPathVertex<Integer>> tmp = previousColumnVertexArray;
            previousColumnVertexArray = currentColumnVertexArray;
            currentColumnVertexArray = tmp;
            //clear it for next column use
            currentColumnVertexArray.clear();
        }
        //最后一排每个顶点建立到<#终点#>的路径
        previousColumnVertexArray.forEach( (aLastColumnVertex) -> aLastColumnVertex.connectToEndVertex( gen.generateDistance() ) );

        System.gc();
        //至此建立了完全的动态规划图,求解之
        long t1 = System.currentTimeMillis();
        NMShortPath<Integer> resultNMShortPath = graphModel.calculateNMShortPath();
        long t2 = System.currentTimeMillis();

        System.out.println( "求解动态规划 spend: " + (t2 - t1) + " ms\n\n" );
        System.out.flush();
        //打印看下结果
        System.err.println( "NM-最短路径(N==" + N + ", M=" + M + "):" );
        System.err.println( "    实际ShortPath个数(N)为[" + resultNMShortPath.getShortPathCount() + "]个" );
        System.err.println( "    实际VertexPath个数(M)所有从起点到终点的经由不同顶点的路径顶点序列有[" + resultNMShortPath.getVertexPathCount() + "]个" );
        /*
        System.err.println( "详细路径信息：" );
        for (int i = 0, shortPathCount = resultNMShortPath.getShortPathCount(); i < shortPathCount; ++i) {
            ShortPath<Integer> nextShortPath = resultNMShortPath.getShortPathAt( i );
            System.err.println( "    第[" + i + "]个ShortPath: 路径长度[" + nextShortPath.getTotalDistance() + "], 包含的不同顶点序列路径有[" + nextShortPath.getVertexPathCount() + "]个:" );
            for (int j = 0, vertextPathCount = nextShortPath.getVertexPathCount(); j < vertextPathCount; ++j) {
                VertexPath<Integer> nextVertexPath = nextShortPath.getVertexPathAt( j );
                System.err.print( "        第[" + j + "]个顶点路径序列:" );
                for (int k = 0, vertextCount = nextVertexPath.getVertexCount(); k < vertextCount; ++k) {
                    if (k == vertextCount - 1) {
                        System.err.print( "[顶点-" + nextVertexPath.getVertexAt( k ).getAttachment() + "]" );
                    }
                    else {
                        System.err.print( "[顶点-" + nextVertexPath.getVertexAt( k ).getAttachment() + "]--(" + nextVertexPath.getDistanceBetween( k, k + 1 ) + ")-->" );
                    }
                }
                System.err.println();
            }
        }
        */
    }

    static final int mColumnCounter = DemoDynamicProgramming.ColumnCount * DemoDynamicProgramming.VertexCountOfEveryColumn;
    static AtomicInteger mDistanceGenerator = new AtomicInteger( DemoDynamicProgramming.mColumnCounter * 1000 );

    @FunctionalInterface
    interface DebugDistanceGenerator {
        double generateDistance();
    }

    /**
     * 所有边的距离都是0.7：此时哪怕 N==1 也产生组合爆炸问题：超指数增长的不同顶点路径的组合数目增长。
     * 此时固定N==1，此时可把 M 逐步调大来看爆炸的影响。如果N为其他数，那爆炸的更快了。
     */
    public static DebugDistanceGenerator mFixedGenerator = () -> 0.7;

    /**
     * 一列内部各顶点相同，但一个顶点的各个边不同：固定N后看 M 的逐步影响
     */
    public static DebugDistanceGenerator mVertexSameGenerator = () -> {
        int distance = DemoDynamicProgramming.mDistanceGenerator.decrementAndGet();
        if ((distance % (DemoDynamicProgramming.VertexCountOfEveryColumn)) == 0) {
            DemoDynamicProgramming.mDistanceGenerator.set( DemoDynamicProgramming.mColumnCounter * 1000 );
        }
        return distance + 0.7;
    };

    /**
     * 每一列的顶点的边距离相同，但一列内部的各顶点不相同：固定N后看 M 的逐步影响
     */
    public static DebugDistanceGenerator mColumnSameGenerator = () -> {
        int distance = DemoDynamicProgramming.mDistanceGenerator.decrementAndGet();
        if ((distance % (DemoDynamicProgramming.mColumnCounter)) == 0) {
            DemoDynamicProgramming.mDistanceGenerator.set( DemoDynamicProgramming.mColumnCounter * 1000 );
        }
        return distance + 0.7;
    };

    /**
     * 所有边的距离都不相同：此时固定N后， M 几乎没有什么影响，因为每一个ShortPath 基本只有一条 VertexPath。
     *
     * 问题能否求解（发生OOM）取决于 N 的大小, 此时把 M 设置成多大影响也不大。
     */
    public static DebugDistanceGenerator mAllNotSameGenerator = () -> DemoDynamicProgramming.mDistanceGenerator.getAndDecrement() + 0.7;
}
