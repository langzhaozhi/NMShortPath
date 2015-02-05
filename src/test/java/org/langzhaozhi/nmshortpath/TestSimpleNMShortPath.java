package org.langzhaozhi.nmshortpath;

import org.langzhaozhi.nmshortpath.NMShortPath.ShortPath;
import org.langzhaozhi.nmshortpath.NMShortPath.VertexPath;

/**
 * NM-最短路径测试：创建A,B,C,D,E顶点，加上起点和终点共7个顶点，求解前N个最短路径。
 */
public class TestSimpleNMShortPath {
    private static final int N = 1000;
    private static final int M = 1000;

    @SuppressWarnings("unchecked")
    public static void main(String [] args) {
        //NM-最短路径图模型, 这里的N==1000,M=1000
        NMShortPathGraph<String> graphModel = new NMShortPathGraph<String>( N, M, "<#起点#>", "<#终点#>" );

        //从起点到顶点A距离为1
        NMShortPathVertex<String> vertexA = new NMShortPathVertex<String>( "<顶点A>", new NMShortPathEdge<String>( graphModel.getStartVertex(), 1 ) );

        //从顶点A到顶点B距离为1
        NMShortPathVertex<String> vertexB = new NMShortPathVertex<String>( "<顶点B>", new NMShortPathEdge<String>( vertexA, 1 ) );

        //从顶点A到顶点C距离为2，从顶点B到顶点C距离为1
        NMShortPathVertex<String> vertexC = new NMShortPathVertex<String>( "<顶点C>", new NMShortPathEdge<String>( vertexA, 2 ), new NMShortPathEdge<String>( vertexB, 1 ) );

        //顶点B到顶点D距离为1，顶点C到顶点D距离为1
        NMShortPathVertex<String> vertexD = new NMShortPathVertex<String>( "<顶点D>", new NMShortPathEdge<String>( vertexB, 1 ), new NMShortPathEdge<String>( vertexC, 1 ) );

        //顶点D到顶点E距离为1
        NMShortPathVertex<String> vertexE = new NMShortPathVertex<String>( "<顶点E>", new NMShortPathEdge<String>( vertexD, 1 ) );

        //顶点E到终点距离为1
        vertexE.connectToEndVertex( 1 );
        //顶点D到终点距离为3
        vertexD.connectToEndVertex( 3 );
        //顶点C到终点距离为2
        vertexC.connectToEndVertex( 2 );

        NMShortPath<String> resultNMShortPath = graphModel.calculateNMShortPath();
        System.err.println( "NM-最短路径(N==" + N + ", M=" + M + "):" );
        System.err.println( "    实际ShortPath个数(N)为[" + resultNMShortPath.getShortPathCount() + "]个" );
        System.err.println( "    实际VertexPath个数(M)所有从起点到终点的经由不同顶点的路径顶点序列有[" + resultNMShortPath.getVertexPathCount() + "]个" );
        System.err.println( "详细路径信息：" );
        for (int i = 0, shortPathCount = resultNMShortPath.getShortPathCount(); i < shortPathCount; ++i) {
            ShortPath<String> nextShortPath = resultNMShortPath.getShortPathAt( i );
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
