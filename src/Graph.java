import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A simple weighted, directed, graph.
 *
 * @author Samuel A. Rebelsky
 */
public class Graph {

  // +-------+-------------------------------------------------------
  // | Notes |
  // +-------+

  /*
   * We implement our graphs using adjacency lists.  For each
   * vertex, v, we store a list of edges from that vertex.
   *
   * For convenience, you can refer to vertices by number or by
   * name.  However, it is more efficient to refer to them by number.
   */

  // +-----------+---------------------------------------------------
  // | Constants |
  // +-----------+

  /**
   * The default initial capacity (# of nodes) of the graph.
   */
  static final int INITIAL_CAPACITY = 16;

  // +--------+------------------------------------------------------
  // | Fields |
  // +--------+

  /**
   * The number of vertices in the graph.
   */
  int numVertices;

  /**
   * The number of edges in the graph.
   */
  int numEdges;

  /**
   * The vertices in the graph.  The edges from vertex v are
   * stored in vertices[v].
   */
  List<Edge>[] vertices;

  /**
   * The names of the vertices.  The name of vertex v is stored
   * in vertexNames[v].
   */
  String[] vertexNames;

  /**
   * The unused vertices
   */
  Queue<Integer> unusedVertices;

  /**
   * The numbers of the vertices.  The vertex with name n is in
   * vertexNumbers.get(n).
   */
  HashMap<String,Integer> vertexNumbers;

  /**
   * The version of the graph.  (Essentially, the number of times
   * we've modified the graph.)
   */
  long version;

  // +--------------+------------------------------------------------
  // | Constructors |
  // +--------------+

  /**
   * Create a new graph with the default capacity.
   */
  public Graph() {
    this(INITIAL_CAPACITY);
  } // Graph()

  /**
   * Create a new graph with a specified initial capacity.
   */
  @SuppressWarnings("unchecked")
  public Graph(int initialCapacity) {
    this.vertices = (ArrayList<Edge>[]) new ArrayList[initialCapacity];
    this.vertexNames = new String[initialCapacity];
    this.vertexNumbers = new HashMap<String,Integer>();
    this.unusedVertices = new LinkedList<Integer>();
    this.version = 0;
    for (int i = 0; i < this.vertices.length; i++) {
      this.vertices[i] = new ArrayList<Edge>();
      this.unusedVertices.add(i);
    } // for
  } // Graph(int)

  // +----------------------+----------------------------------------
  // | Vertex names/numbers |
  // +----------------------+

  /**
   * Given a vertex number, get the corresponding vertex name.
   * If there is no corresponding vertex name, returns null;
   */
  public String vertexName(int vertexNumber) {
    if ((vertexNumber < 0) || (vertexNumber >= vertexNames.length)) {
      return null;
    } else {
      return this.vertexNames[vertexNumber];
    } // if/else
  } // vertexName(int)

  /**
   * Given a vertex name, get the corresponding vertex number.
   * If there is no corresponding vertex number, returns -1.
   */
  public int vertexNumber(String vertexName) {
    Integer result = vertexNumbers.get(vertexName);
    if (result == null) {
      return -1;
    } else {
      return result;
    } // if/else
  } // vertexNumber(String)

  // +-----------+---------------------------------------------------
  // | Observers |
  // +-----------+

  /**
   * Dump the graph in a not very useful way.
   */
  public void dump(PrintWriter pen) {
    pen.println("A Graph");
    pen.println("  with " + numVertices + " vertices");
    pen.println("  and " + numEdges + " edges");
    for (int vertex = 0; vertex < vertices.length; vertex++) {
      if (validVertex(vertex)) {
        pen.print(vertex + ": ");
        for (Edge e : vertices[vertex]) {
          pen.print(e + " ");
        } // for()
        pen.println();
      } // if
    } // for
    pen.println();
  } // dump()
  
  /**
   * Get the number of edges.
   */
  public int numEdges() {
    return this.numEdges;
  } // numEdges()

  /**
   * Get the number of vertices.
   */
  public int numVertices() {
    return this.numVertices;
  } // numVertices

  /**
   * Get an iterator for the edges.
   */
  public Iterator<Edge> edges() {
    return new Iterator<Edge>() {
      // The position of the iterator 
      int pos = 0;
      // The version number of the graph when this iterator was created
      long version = Graph.this.version;
      // The current edge iterator
      Iterator<Edge> ie = Graph.this.vertices[0].iterator();
      // The current vertex
      int vertex = 0;

      /**
       * Determine if edges remain.
       */
      public boolean hasNext() {
        failFast(this.version);
        return this.pos < Graph.this.numEdges;
      } // hasNext()

      /**
       * Grab the next edge
       */
      public Edge next() {
        if (!this.hasNext()) {
          throw new NoSuchElementException();
        }
        while (!this.ie.hasNext()) {
          this.ie = Graph.this.vertices[++this.vertex].iterator();
        } // while
        ++this.pos;
        return ie.next();
      } // next()
    }; // new Iterator<Edge>
  } // edges()

  /**
   * Get all of the edges from a particular vertex.
   */
  public Iterator<Edge> edgesFrom(int vertex) {
    if ((vertex < 0) || (vertex >= this.vertices.length)) {
      return new Iterator<Edge>() {
        long version = Graph.this.version;
        public boolean hasNext() {
          failFast(this.version);
          return false;
        } // hasNext()
        public Edge next() {
          failFast(this.version);
          throw new NoSuchElementException();
        } // next()
      }; // new Iterator<Edge>
    } else {
      return new Iterator<Edge>() {
        // The version number of the graph when this iterator was created
        long version = Graph.this.version;
        // The underlying iterator.  We wrap it so that the client
        // cannot call the remove method.
        Iterator<Edge> edges = Graph.this.vertices[vertex].iterator();

        public boolean hasNext() {
          failFast(this.version);
          return edges.hasNext();
        } // hasNext()

        public Edge next() {
          failFast(this.version);
          return edges.next();
        } // next()
      }; // new Iterator<Edge>
    } // if else
  } // edgesFrom(int)

  /**
   * Get all of the edges from a particular vertex.
   */
  public Iterator<Edge> edgesFrom(String vertex) {
    return this.edgesFrom(vertexNumber(vertex));
  } // edgesFrom(String)

  /**
   * Get an iterator for the vertices.
   */
  public Iterator<Integer> vertices() {
    return new Iterator<Integer>() {
      // The position of the iterator
      int pos = 0;
      // The version number of the graph when this iterator was created
      long version = Graph.this.version;
      // The current vertex number
      int vertex = 0;

      /**
       * Determine if vertices remain.
       */
      public boolean hasNext() {
        failFast(this.version);
        return this.pos < Graph.this.numVertices;
      } // hasNext()

      /**
       * Grab the next vertex.
       */
      public Integer next() {
        if (!this.hasNext()) {
          throw new NoSuchElementException();
        } // if
        while (Graph.this.vertexNames[this.vertex] == null) {
          ++this.vertex;
        } // while
        return this.vertex++;
      } // next()
    }; // new Iterator<Integer>
  } // vertices()
  
 public void reachableFromDFS(PrintWriter pen, int vertex) {
    // get the list of edges coming out vertex
    // List<Edge> listEdges = vertices[vertex];
    boolean[] visited = new boolean[vertices.length];
    for (int i = 0; i < visited.length; i++) {
      visited[i] = false;
    }

    // if we dont initialize the first vertex as true in visited,
    // it will take an extra step
    // and when the graph has one vertex, visited[i] will not be set correctly

    // A collection of the remaining things to print
    Stack<Integer> remaining = new Stack<Integer>();
    // push the vertex on the stack
    remaining.push(vertex);

    while (!remaining.isEmpty()) {
      // popping the root in first iteration
      int next = remaining.pop();
      // if the vertex was not visited
      if (visited[next] == false) {
        visited[next] = true;
        // get all adjacent vertices (depth first) and explore it
        Iterator<Edge> itr = edgesFrom(next);
        while (itr.hasNext()) {
          int vertexReached = itr.next().to();
          if (!visited[vertexReached]) {
            remaining.push(vertexReached);
            // cant set visited[vertexReached] to true here 
            // because only when we pop, the vertex is visited (or we explored it)
            // else we just seen it
            //visited[vertexReached] = true;
          }
        }

      }

    } // while
    for (int i = 0; i < visited.length; i++) {
      if (visited[i]) {
        pen.println(i);
      }
    }
  }
  
  /*  reachableFrom implemented recursively
   * 
   * 
   * First idea: 
   *    * Base case: when the current node is already visited, return
   *    * Inductive case: Else find all the adjacent vertices
   *                use DFS to reach all neighbor of those vertices
   *     Pros : better logic, better to debug
   *     Cons: not efficient: For example:
   *        Graph : 1 --->2--->3---> 1
   *        We need to reach 1 to find out it was already visited
   *        But we actually can know it when we are at 3 because the edge coming out of 3 next goes back to 1
   * Second idea: Collapse base case into the iteration
   *     * create the iterator of all edges coming out of vertex
   *     * while hasNext(), 
   *        * Base case: if vertex was visited, do nothing
   *        * Else call the recursive procedure on the vertex and the boolean array visited 
   *     Pros: shorter, we will never reach the node that will be repeated, because the 
   *     the if condition inside the iteration prevents it
   *        
   */
  public void reachableFromRecHelper(int vertex, boolean[] visited) {
    visited[vertex] = true;
    System.out.println("vertex " + vertex);

    Iterator<Edge> itr = edgesFrom(vertex);
    while (itr.hasNext()) {
      int vertexReached = itr.next().to();
      if (!visited[vertexReached]) {
        reachableFromRecHelper(vertexReached, visited);
      }

    }
  }
  public void reachableFromRec(int vertex) {
    // initialize the boolean array
    boolean[] visited = new boolean[vertices.length];
    for (int i = 0; i < visited.length; i++) {
      visited[i] = false;
    }   
    reachableFromRecHelper(vertex, visited);
    
  }

  
   public Iterator<Integer> iterator(int vertex) {
    return new Iterator<Integer>() {
      Queue<Integer> remaining = new LinkedList<Integer>();
      Integer current = vertex;
      boolean[] visited = new boolean[vertices.length];
      
      public boolean hasNext() {  
        return !remaining.isEmpty();
      } // hasNext()

      public Integer next() {

        if (!hasNext()) {
          try {
            throw new Exception("No more elements");
          } catch (Exception e) {
            // TODO Auto-generated catch block
            
          }
        }

        int next = remaining.remove();
        if (visited[next] == false) {
          visited[next] = true;
          // get all adjacent vertices (depth first) and explore it
          Iterator<Edge> itr = edgesFrom(next);
          while (itr.hasNext()) {
            int vertexReached = itr.next().to();
            if (!visited[vertexReached]) {
              remaining.add(vertexReached);
            }
          }
        }
        return next;
      }// next()
    }; // new Iterator()
  } // iterator()

  // +----------+----------------------------------------------------
  // | Mutators |
  // +----------+

  /**
   * Add an edge between two vertices.  If the edge already exists,
   * replace it.  If the vertices are invalid, throws an exception.
   */
  public void addEdge(int from, int to, int weight) throws Exception {
    if (!validVertex(from) || !validVertex(to)) {
      throw new Exception("Invalid ends");
    } // if
    if (from == to) {
      throw new Exception("Cannot add an edge from a vertex to itself");
    }
    ++this.version;
    Edge newEdge = new Edge(from, to, weight);
    ListIterator<Edge> edges = this.vertices[from].listIterator();
    while (edges.hasNext()) {
      if (edges.next().to() == to) {
        edges.set(newEdge);
        return;
      } // if
    } // while
    edges.add(newEdge);
    ++this.numEdges;
  } // addEdge(int, int, int)

  /**
   * Add an edge between two vertices.  If the edge already exists,
   * replace it.  if the vertices are invalid, throws an exception.
   */
  public void addEdge(String from, String to, int weight) throws Exception {
    addEdge(this.vertexNumber(from), this.vertexNumber(to), weight);
  } // addEdge(String, String, int)

  /**
   * Add a vertex with a particular name.
   *
   * @return v the number of the vertex
   *
   * @exception Exception if there is already a vertex with that name.
   */
  public int addVertex(String name) throws Exception {
    if (vertexNumbers.get(name) != null) {
      throw new Exception("Already have a node named " + name);
    } // if
    return addVertex(name, this.newVertexNumber());
  } // addVertex(String)

  /**
   * Add an unnamed vertex.
   *
   * @return v the number of the vertex
   */
  public int addVertex() {
    int v = this.newVertexNumber();
    String name = "v" + v;
    // On the off chance there is already a vertex with that name,
    // we try some other names.
    while (this.vertexNumbers.get(name) != null) {
      name = "v" + name;
    } // while
    return addVertex(name, v);
  } // addVertex()

  /**
   * Remove an edge.  If the edge does not exist, does nothing.
   */
  public void removeEdge(int from, int to) {
    Iterator<Edge> ie = this.vertices[from].iterator();
    while (ie.hasNext()) {
      if (ie.next().to() == to) {
        ie.remove();
        --this.numEdges;
        ++this.version;
        // We could probably break out of the loop at this point,
        // but it's safer to go through the whole list.
      } // if
    } // while
  } // removeEdge(int, int)

  /**
   * Remove an edge.  If the edge does not exist, does nothing.
   */
  public void removeEdge(String from, String to) {
    removeEdge(this.vertexNumber(from), this.vertexNumber(to));
  } // removeEdge(String, String)

  /**
   * Remove a vertex.  If the vertex does not exist, does nothing.
   */
  public void removeVertex(int vertex) {
    // Ignore pointless vertex numbers
    if (!validVertex(vertex)) {
      return;
    } // if

    // Note the change to the graph
    ++this.version;
    --this.numVertices;
    this.numEdges -= this.vertices[vertex].size();

    // Clear out the entries associated with the vertex
    this.vertices[vertex].clear();
    this.vertexNames[vertex] = null;

    // Clear out edges to that vertex
    for (int i = 0; i < this.vertices.length; i++) {
      Iterator<Edge> ie = this.vertices[i].iterator();
      while (ie.hasNext()) {
        if (ie.next().to() == vertex) {
          ie.remove();
          --this.numEdges;
        } // if
      } // while
    } // for

    // Note that the vertex is once again available to use.
    this.unusedVertices.add(vertex);
  } // removeVertex(int)

  /**
   * Remove a vertex.  If the vertex does not exist, does nothing.
   */
  public void removeVertex(String vertex) {
    this.removeVertex(this.vertexNumber(vertex));
  } // removeVertex(String)

  // +-----------+---------------------------------------------------
  // | Utilities |
  // +-----------+
 
  /**
   * Add a vertex name / vertex number pair.
   *
   * Assumes neither the name or number have been used.
   */
  private int addVertex(String name, int v) {
    ++this.version;
    ++this.numVertices;
    this.vertexNumbers.put(name, v);
    this.vertexNames[v] = name;
    return v;
  } // addVertex(String, int)
    
  /**
   * Expand the necessary arrays.
   */
  private void expand() {
    int oldSize = this.vertices.length;
    int newSize = oldSize * 2;
    this.vertexNames = Arrays.copyOf(this.vertexNames, newSize);
    this.vertices = Arrays.copyOf(this.vertices, newSize);
    for (int i = oldSize; i < newSize; i++) {
      this.vertices[i] = new ArrayList<Edge>();
      this.unusedVertices.add(i);
    } // for
  } // expand()

  /**
   * Compare an expected version to the current version.  Die
   * if they do not match.  (Used to implement the traditional
   * "fail fast" policy for iterators.)
   */
  private void failFast(long expectedVersion) {
    if (this.version != expectedVersion) {
      throw new ConcurrentModificationException();
    } // if
  } // failFast(int)

  /**
   * Determine if a vertex is valid.
   */
  private boolean validVertex(int vertex) {
    return ((vertex >= 0) && (vertex < this.vertices.length) &&
        (this.vertexNames[vertex] != null));
  } // validVertex

  /**
   * Get the next unused vertex number.
   */
  private int newVertexNumber() {
    if (this.unusedVertices.isEmpty()) {
      this.expand();
    }
    return this.unusedVertices.remove();
  } // newVertexNumber()

} // class Graph
