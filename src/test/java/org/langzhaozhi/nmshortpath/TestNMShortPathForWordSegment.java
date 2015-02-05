package org.langzhaozhi.nmshortpath;

import org.langzhaozhi.nmshortpath.NMShortPath.ShortPath;
import org.langzhaozhi.nmshortpath.NMShortPath.VertexPath;

/**
 * NM-最短路径在分词中的简单应用，提供两种创建图模型的方法：<ol>
 *     <li>一种直接创建顶点，每创建一个顶点需要明确传递其所有前向顶点的边，这种方式最不容易出错，因为其顶点关系是严格顺序定义的，
 *     但代码编写起来比较复杂，看起来稍微不那么直观。</li>
 *     <li>包装方式，使用NShortPathGraphWrapper先创建所有的顶点（包括起点和终点），接着再逐步创建顶点之间边的关系，代码看起来简单一些，
 *     </li>
 * </ol>
 */
public class TestNMShortPathForWordSegment {
    private static final int N = 1000;
    private static final int M = 1000;

    public static void main(String [] args) {
        System.err.println( "先采用直接创建NShortPathGraph的方式：" );
        TestNMShortPathForWordSegment.directCreateGraphModel();

        System.err.println( "\n\n\n再对比看下包装方式创建图模型：" );
        TestNMShortPathForWordSegment.wrapperCreateGraphModel();
    }

    @SuppressWarnings("unchecked")
    public static void directCreateGraphModel() {
        //<#起点#>, "商", "商品", "品", "和", "和服", "服", "服务", "务", <#终点#>
        //N-最短路径图模型, 这里的N 是 1000
        NMShortPathGraph<String> graphModel = new NMShortPathGraph<String>( N, M, "<#起点#>", "<#终点#>" );

        //<起点>
        NMShortPathVertex<String> vertex0 = graphModel.getStartVertex();
        //(起点,1)距离 4.18
        NMShortPathVertex<String> vertex1 = new NMShortPathVertex<String>( "<商>", new NMShortPathEdge<String>( vertex0, 4.18 ) );
        //(起点,2)距离 4.18
        NMShortPathVertex<String> vertex2 = new NMShortPathVertex<String>( "<商品>", new NMShortPathEdge<String>( vertex0, 4.18 ) );
        //(1,3)距离12.06
        NMShortPathVertex<String> vertex3 = new NMShortPathVertex<String>( "<品>", new NMShortPathEdge<String>( vertex1, 12.06 ) );
        //(2,4)距离3.59, (3,4)距离12.44
        NMShortPathVertex<String> vertex4 = new NMShortPathVertex<String>( "<和>", new NMShortPathEdge<String>( vertex2, 3.59 ), new NMShortPathEdge<String>( vertex3, 12.44 ) );
        //(2,5)距离9.63,(3,5)距离12.44
        NMShortPathVertex<String> vertex5 = new NMShortPathVertex<String>( "<和服>", new NMShortPathEdge<String>( vertex2, 9.63 ), new NMShortPathEdge<String>( vertex3, 12.44 ) );
        //(4,6)距离5.70
        NMShortPathVertex<String> vertex6 = new NMShortPathVertex<String>( "<服>", new NMShortPathEdge<String>( vertex4, 5.70 ) );
        //(4,7)距离5.14
        NMShortPathVertex<String> vertex7 = new NMShortPathVertex<String>( "<服务>", new NMShortPathEdge<String>( vertex4, 5.14 ) );
        //(5,8)距离14.22,(6,8)距离12.54
        NMShortPathVertex<String> vertex8 = new NMShortPathVertex<String>( "<务>", new NMShortPathEdge<String>( vertex5, 14.22 ), new NMShortPathEdge<String>( vertex6, 12.54 ) );

        //(7,终点)距离4.95
        vertex7.connectToEndVertex( 4.95 );
        //(8,终点)距离13.66
        vertex8.connectToEndVertex( 13.66 );

        NMShortPath<String> resultNMShortPath = graphModel.calculateNMShortPath();

        TestNMShortPathForWordSegment.outputNShortPath( resultNMShortPath );
    }

    private static void wrapperCreateGraphModel() {
        String [] attchments = {
            "<#起点#>", "商", "商品", "品", "和", "和服", "服", "服务", "务", "<#终点#>"
        };
        NMShortPathGraphWrapper<String> graphModelWrapper = new NMShortPathGraphWrapper<String>( N, M, attchments );
        graphModelWrapper.createEdge( 0, 1, 4.18 ).createEdge( 0, 2, 4.18 ).createEdge( 1, 3, 12.06 ).createEdge( 2, 4, 3.59 ).createEdge( 3, 4, 12.44 );
        graphModelWrapper.createEdge( 2, 5, 9.63 ).createEdge( 3, 5, 12.44 ).createEdge( 4, 6, 5.70 ).createEdge( 4, 7, 5.14 ).createEdge( 5, 8, 14.22 );
        graphModelWrapper.createEdge( 6, 8, 12.54 ).createEdge( 7, 9, 4.95 ).createEdge( 8, 9, 13.66 );

        TestNMShortPathForWordSegment.outputNShortPath( graphModelWrapper.calculateNShortPath() );
    }

    private static void outputNShortPath(NMShortPath<String> aResultNMShortPath) {
        System.err.println( "NM-最短路径(N==" + N + ", M=" + M + "):" );
        System.err.println( "    实际ShortPath个数(N)为[" + aResultNMShortPath.getShortPathCount() + "]个" );
        System.err.println( "    实际VertexPath个数(M)所有从起点到终点的经由不同顶点的路径顶点序列有[" + aResultNMShortPath.getVertexPathCount() + "]个" );
        System.err.println( "详细路径信息：" );
        for (int i = 0, shortPathCount = aResultNMShortPath.getShortPathCount(); i < shortPathCount; ++i) {
            ShortPath<String> nextShortPath = aResultNMShortPath.getShortPathAt( i );
            System.err.println( "    第[" + i + "]个ShortPath: 路径长度[" + nextShortPath.getTotalDistance() + "], 包含的不同顶点序列路径有[" + nextShortPath.getVertexPathCount() + "]个:" );
            for (int j = 0, vertextPathCount = nextShortPath.getVertexPathCount(); j < vertextPathCount; ++j) {
                VertexPath<String> nextVertexPath = nextShortPath.getVertexPathAt( j );
                System.err.print( "        第[" + j + "]个顶点路径序列:" );
                for (int k = 0, vertextCount = nextVertexPath.getVertexCount(); k < vertextCount; ++k) {
                    if (k == vertextCount - 1) {
                        System.err.print( nextVertexPath.getVertexAt( k ).getAttachment() );
                    }
                    else {
                        System.err.print( nextVertexPath.getVertexAt( k ).getAttachment() + "--(" + nextVertexPath.getDistanceBetween( k, k + 1 ) + ")-->" );
                    }
                }
                System.err.println();
            }
        }
    }
}
