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
    private static final int ColumnCount = 300;
    private static final int VertexCountOfEveryColumn = 100;
    private static final int N = 100;//NM-最短路径的N可以依次调 N=1,2,3.....看下情况;
    private static final int M = 1000;//当M调节的愈来愈大的时候，可以明显看到组合爆炸的问题：表现为内存占用和求解时间的爆炸伤

    public static void main(String [] args) {
        AtomicInteger attachmentGenerator = new AtomicInteger( 0 );
        AtomicInteger distanceGenerator = new AtomicInteger( 1000_0000 );

        NMShortPathGraph<Integer> graphModel = new NMShortPathGraph<Integer>( N, M, attachmentGenerator.getAndIncrement(), 0xFFFF_FFFF );

        //初始<#起点#>为前一排的顶点
        ArrayList<NMShortPathVertex<Integer>> previousColumnVertexArray = new ArrayList<NMShortPathVertex<Integer>>( DemoDynamicProgramming.VertexCountOfEveryColumn );
        previousColumnVertexArray.add( graphModel.getStartVertex() );

        ArrayList<NMShortPathVertex<Integer>> currentColumnVertexArray = new ArrayList<NMShortPathVertex<Integer>>( DemoDynamicProgramming.VertexCountOfEveryColumn );

        for (int i = 0; i < DemoDynamicProgramming.ColumnCount; ++i) {
            for (int j = 0; j < DemoDynamicProgramming.VertexCountOfEveryColumn; ++j) {
                distanceGenerator.set( 10000 );//前一排的每个顶点到当前排的每个顶点都有一条前向边:距离依次递减
                @SuppressWarnings("unchecked")
                NMShortPathEdge<Integer> [] previousEdgeArray = previousColumnVertexArray.stream().map( (aPreviousVertex) -> new NMShortPathEdge<Integer>( aPreviousVertex, distanceGenerator.getAndDecrement() + 0.7 ) ).toArray( NMShortPathEdge []::new );
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
        previousColumnVertexArray.forEach( (aLastColumnVertex) -> aLastColumnVertex.connectToEndVertex( distanceGenerator.getAndDecrement() ) );

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
}
