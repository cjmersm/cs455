package cs455.cdn.mst;

// Taken from http://krishami.com/2011/12/implementation-of-kruskals-algorithm-in-java/

public class Edge implements Comparable<Edge>
{
    String vertexA, vertexB;
    int weight;

    public Edge(String vertexA, String vertexB, int weight)
    {
        this.vertexA = vertexA;
        this.vertexB = vertexB;
        this.weight = weight;
    }
    public String getVertexA()
    {
        return vertexA;
    }
    public String getVertexB()
    {
        return vertexB;
    }
    public int getWeight()
    {
        return weight;
    }
    @Override
    public String toString()
    {
        return "(" + vertexA + "\t" + vertexB + ") : Weight = " + weight;
    }
    public int compareTo(Edge edge)
    {
        //== is not compared so that duplicate values are not eliminated.
        return (this.weight < edge.weight) ? -1: 1;
    }
}
