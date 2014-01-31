package cs455.cdn.mst;

import java.util.TreeSet;

public class Kruskal
{
    public static void main(String[] args)
    {
        //TreeSet is used to sort the edges before passing to the algorithm
        TreeSet<Edge> edges = new TreeSet<Edge>();

        //Sample problem - replace these values with your problem set
        edges.add(new Edge("B;129.82.47.215;40596;53450", "A;129.82.47.215;40596;53450", 2));
        edges.add(new Edge("0", "3", 1));
        edges.add(new Edge("1", "2", 3));
        edges.add(new Edge("2", "3", 5));
        edges.add(new Edge("2", "4", 7));
        edges.add(new Edge("3", "4", 6));
        edges.add(new Edge("4", "5", 4));

        System.out.println("Graph");
        KruskalEdges vv = new KruskalEdges();

        for (Edge edge : edges) {
            System.out.println(edge);
            vv.insertEdge(edge);
        }

        System.out.println("Kruskal algorithm");
        int total = 0;
        for (Edge edge : vv.getEdges()) {
            System.out.println(edge);
            total += edge.getWeight();
        }
        System.out.println("Total weight is " + total);
    }
}