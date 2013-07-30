package com.planet_ink.coffee_mud.core.collections;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 2D R-Tree implementation for Android.
 * Uses algorithms from: http://www.sai.msu.su/~megera/postgres/gist/papers/Rstar3.pdf
 * @author Colonel32
 * @author cnvandev
 * 
 * Adapted to 3d by:
 * @author Bo Zimmerman
 * 
 * Found at:
 * https://github.com/pruby/xppylons/blob/master/src/com/untamedears/xppylons/rtree/AABB.java
 * (No license found)
 */
public class RTree {
	private RTreeNode root;
	private int maxSize;
	private int minSize;
	private QuadraticNodeSplitter splitter;
	
	public static boolean intersects(CubeL one, CubeL two)
	{
		return 	  one.contains(two.lx,two.ty,two.iz)
				||one.contains(two.rx,two.ty,two.iz)
				||one.contains(two.lx,two.by,two.iz)
				||one.contains(two.rx,two.by,two.iz)
				||one.contains(two.lx,two.ty,two.oz)
				||one.contains(two.rx,two.ty,two.oz)
				||one.contains(two.lx,two.by,two.oz)
				||one.contains(two.rx,two.by,two.oz);
	}
	
	public static class CubeL
	{
		private long lx,ty,iz =0;
		private long rx,by,oz=0;
		
		public CubeL()
		{
			super();
		}
		
		public CubeL(long lx, long rx, long ty, long by, long iz, long oz)
		{
			super();
			this.lx=lx; this.rx=rx;
			this.ty=ty; this.by=by;
			this.iz=iz; this.oz=oz;
		}
		
		public CubeL(CubeL l)
		{
			super();
			set(l);
		}
		
		public void set(CubeL l)
		{
			this.lx=l.lx; this.rx=l.rx;
			this.ty=l.ty; this.by=l.by;
			this.iz=l.iz; this.oz=l.oz;
		}
		public void union(CubeL l)
		{
			if(l.lx < lx) lx=l.lx;
			if(l.rx > rx) rx=l.rx;
			if(l.ty < ty) ty=l.ty;
			if(l.by > by) by=l.by;
			if(l.iz < iz) iz=l.iz;
			if(l.oz > oz) oz=l.oz;
		}
		
		public boolean contains(long x, long y, long z)
		{
			return ((x>=lx)&&(x<=rx)
			&&(y>=ty)&&(y<=by)
			&&(z>=iz)&&(z<=oz));
		}
		
		public long width() { return rx-lx; }
		public long height() { return by-ty; }
		public long depth() { return oz-iz; }
	}
	
	public static interface BoundedObject
	{
		public CubeL getBounds();
	}
	
	public static interface RTreeViewable extends BoundedObject
	{
	}
	
	public class RTreeNode implements BoundedObject {
		RTreeNode parent;
		CubeL box;
		ArrayList<RTreeNode> children;
		ArrayList<RTreeViewable> data;
		
		public RTreeNode() {}

		public RTreeNode(boolean isLeaf)	{
			if (isLeaf) {
				data = new ArrayList<RTreeViewable>(maxSize+1);
			} else {
				children = new ArrayList<RTreeNode>(maxSize+1);
			}
		}

		public boolean isLeaf() {
			return data != null;
		}
		
		public boolean isRoot() {
			return parent == null;
		}

		public void addTo(RTreeNode parent) {
			assert(parent.children != null);
			parent.children.add(this);
			this.parent = parent;
			computeMBR();
			splitter.split(parent);
		}

		public void computeMBR() {
			computeMBR(true);
		}

		public void computeMBR(boolean doParents) {
			if (box == null)
				box = new CubeL();
			
			if (!isLeaf()) {
				if (children.isEmpty())
					return;
				
				box.set(children.get(0).box);
				for (int i = 1; i < children.size(); i++) {
					box.union(children.get(i).box);
				}
			} else {
				if (data.isEmpty())
					return;
				
				box.set(data.get(0).getBounds());
				for (int i = 1; i < data.size(); i++) {
					box.union(data.get(i).getBounds());
				}
			}

			if (doParents && parent != null)
				parent.computeMBR();
		}

		public void remove() {
			if (parent == null) {
				assert(root == this);
				root = null;
				return;
			}
			
			parent.children.remove(this);
			
			if (parent.children.isEmpty()) {
				parent.remove();
			} else {
				parent.computeMBR();
			}
		}

		public ArrayList<? extends BoundedObject> getSubItems() {
			return isLeaf() ? data : children;
		}

		public CubeL getBounds() {
			return box;
		}
		
		public boolean contains(long px, long py, int pz) {
			return box.contains(px, py, pz);
		}

		public int size() {
			return isLeaf() ? data.size() : children.size();
		}
		
		public int depth() {
			RTreeNode n = this;
			int d = 0;
			while(n != null) {
				n = n.parent;
				d++;
			}
			return d;
		}

		public String toString() {
			return "Depth: "+depth()+", size: "+size();
		}
	}
	
	private class QuadraticNodeSplitter {
		public void split(RTreeNode n) {
			if (n.size() <= maxSize) return;
			boolean isleaf = n.isLeaf();

			// Choose seeds. Would write a function for this, but it requires returning 2 objects
			BoundedObject seed1 = null, seed2 = null;
			ArrayList<? extends BoundedObject> list;
			if (isleaf)
				list = n.data;
			else
				list = n.children;

			long maxD = Long.MIN_VALUE;
			CubeL box = new CubeL();
			for (int i = 0; i < list.size(); i++) {
				for (int j=0; j<list.size(); j++) {
					if (i == j) continue;
					BoundedObject n1 = list.get(i), n2 = list.get(j);
					box.set(n1.getBounds());
					box.union(n2.getBounds());
					long d = area(box) - area(n1.getBounds()) - area(n2.getBounds());
					if (d > maxD) {
						maxD = d;
						seed1 = n1;
						seed2 = n2;
					}
				}
			}
			if((seed1==null)||(seed2==null))
			{
				assert(seed1 != null && seed2 != null);
				return;
			}

			// Distribute
			RTreeNode group1 = new RTreeNode(isleaf);
			group1.box = new CubeL(seed1.getBounds());
			RTreeNode group2 = new RTreeNode(isleaf);
			group2.box = new CubeL(seed2.getBounds());
			if (isleaf)
				distributeLeaves(n, group1, group2);
			else
				distributeBranches(n, group1, group2);

			RTreeNode parent = n.parent;
			if (parent == null) {
				parent = new RTreeNode(false);
				root = parent;
			} else {
				parent.children.remove(n);
			}

			group1.parent = parent;
			parent.children.add(group1);
			group1.computeMBR();
			split(parent);

			group2.parent = parent;
			parent.children.add(group2);
			group2.computeMBR();
			split(parent);
		}
		
		private void distributeBranches(RTreeNode n, RTreeNode g1, RTreeNode g2) {
			assert(!(n.isLeaf() || g1.isLeaf() || g2.isLeaf()));

			while(!n.children.isEmpty() && g1.children.size() < maxSize - minSize + 1 && g2.children.size() < maxSize - minSize + 1) {
				// Pick next
				int difmax = Integer.MIN_VALUE;
				int nmax_index = -1;
				for (int i = 0; i < n.children.size(); i++) {
					RTreeNode node = n.children.get(i);
					int expansion1 = expansionNeeded(node.box, g1.box);
					int expansion2 = expansionNeeded(node.box, g2.box);
					int dif = Math.abs(expansion1 - expansion2);
					if (dif > difmax) {
						difmax = dif;
						nmax_index = i;
					}
				}
				assert(nmax_index != -1);

				// Distribute Entry
				RTreeNode nmax = n.children.remove(nmax_index);
				RTreeNode parent = null;

				// ... to the one with the least expansion
				int overlap1 = expansionNeeded(nmax.box, g1.box);
				int overlap2 = expansionNeeded(nmax.box, g2.box);
				if (overlap1 > overlap2) {
					parent = g1;
				} else if (overlap2 > overlap1) {
					parent = g2;
				} else {
					// Or the one with the lowest area
					long area1 = area(g1.box);
					long area2 = area(g2.box);
					if (area1 > area2) parent = g2;
					else if (area2 > area1) parent = g1;
					else {
						// Or the one with the least items
						if (g1.children.size() < g2.children.size()) parent = g1;
						else parent = g2;
					}
				}
				assert(parent != null);
				parent.children.add(nmax);
				nmax.parent = parent;
			}

			if (!n.children.isEmpty()) {
				RTreeNode parent = null;
				if (g1.children.size() == maxSize - minSize + 1)
					parent = g2;
				else
					parent = g1;

				for (int i = 0; i < n.children.size(); i++)	{
					parent.children.add(n.children.get(i));
					n.children.get(i).parent = parent;
				}
				n.children.clear();
			}
		}
		
		private void distributeLeaves(RTreeNode n, RTreeNode g1, RTreeNode g2) {
			// Same process as above; just different types.
			assert(n.isLeaf() && g1.isLeaf() && g2.isLeaf());

			while(!n.data.isEmpty() && g1.data.size() < maxSize - minSize + 1 && g2.data.size() < maxSize - minSize + 1) {
				// Pick next
				int difmax = Integer.MIN_VALUE;
				int nmax_index = -1;
				for (int i = 0; i < n.data.size(); i++) {
					BoundedObject node = n.data.get(i);
					int d1 = expansionNeeded(node.getBounds(), g1.box);
					int d2 = expansionNeeded(node.getBounds(), g2.box);
					int dif = Math.abs(d1 - d2);
					if (dif > difmax) {
						difmax = dif;
						nmax_index = i;
					}
				}
				assert(nmax_index != -1);

				// Distribute Entry
				RTreeViewable nmax = n.data.remove(nmax_index);

				// ... to the one with the least expansion
				int overlap1 = expansionNeeded(nmax.getBounds(), g1.box);
				int overlap2 = expansionNeeded(nmax.getBounds(), g2.box);
				if (overlap1 > overlap2) {
					g1.data.add(nmax);
				} else if (overlap2 > overlap1) {
					g2.data.add(nmax);
				} else {
					long area1 = area(g1.box);
					long area2 = area(g2.box);
					if (area1 > area2) {
						g2.data.add(nmax);
					} else if (area2 > area1) {
						g1.data.add(nmax);
					} else {
						if (g1.data.size() < g2.data.size()) {
							g1.data.add(nmax);
						} else {
							g2.data.add(nmax);
						}
					}
				}
			}

			if (!n.data.isEmpty()) {
				if (g1.data.size() == maxSize - minSize + 1) {
					g2.data.addAll(n.data);
				} else {
					g1.data.addAll(n.data);
				}
				n.data.clear();
			}
		}
	}

	/**
	 * Default constructor.
	 */
	public RTree() {
		this(2, 12);
	}
	
	/**
	 * Creates an R-Tree. Sets the splitting algorithm to quadratic splitting.
	 * @param minChildren Minimum children in a node.  {@code 2 <= minChildren <= maxChildren/2}
	 * @param maxChildren Maximum children in a node. Node splits at this number + 1
	 */
	public RTree(int minChildren, int maxChildren) {
		if (minChildren < 2 || minChildren > maxChildren/2)
			throw new IllegalArgumentException("2 <= minChildren <= maxChildren/2");
		splitter = new QuadraticNodeSplitter();
				
		this.minSize = minChildren;
		this.maxSize = maxChildren;
		root = null;
	}

	/**
	 * Adds items whose AABB intersects the query AABB to results
	 * @param results A collection to store the query results
	 * @param box The query
	 */
	public void query(Collection<RTreeViewable> results) {
		CubeL box = new CubeL(Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);
		query(results, box, root);
	}
	public void query(Collection<RTreeViewable> results, CubeL box) {
		query(results, box, root);
	}
	private void query(Collection<RTreeViewable> results, CubeL box, RTreeNode node) {
		if (node == null) return;
		if (node.isLeaf()) {
			for (int i = 0; i < node.data.size(); i++)
				if (intersects(node.data.get(i).getBounds(), box))
					results.add(node.data.get(i));
		} else {
			for (int i = 0; i < node.children.size(); i++) {
				if (intersects(node.children.get(i).box, box)) {
					query(results, box, node.children.get(i));
				}
			}
		}
	}

	/**
	 * Returns one item that intersects the query box, or null if nothing intersects
	 * the query box.
	 */
	public BoundedObject queryOne(CubeL box) {
		return queryOne(box,root);
	}
	private BoundedObject queryOne(CubeL box, RTreeNode node) {
		if (node == null) return null;
		if (node.isLeaf()) {
			for (int i = 0; i < node.data.size(); i++) {
				if (intersects(node.data.get(i).getBounds(), box)) {
					return node.data.get(i);
				}
			}
			return null;
		} else {
			for (int i = 0; i < node.children.size(); i++) {
				if (intersects(node.children.get(i).box, box)) {
					BoundedObject result = queryOne(box,node.children.get(i));
					if (result != null) return result;
				}
			}
			return null;
		}
	}

	/**
	 * Returns items whose Rect contains the specified point.
	 * @param results A collection to store the query results.
	 * @param px Point X coordinate
	 * @param py Point Y coordinate
	 */
	public void query(Collection<? super BoundedObject> results, long px, long py, long pz) {
		query(results, px, py, pz, root);
	}
	private void query(Collection<? super BoundedObject> results, long px, long py, long pz, RTreeNode node) {
		if (node == null) return;
		if (node.isLeaf()) {
			for (int i = 0; i < node.data.size(); i++) {
				if (node.data.get(i).getBounds().contains(px, py, pz)) {
					results.add(node.data.get(i));
				}
			}
		} else {
			for (int i = 0; i < node.children.size(); i++) {
				if (node.children.get(i).box.contains(px, py, pz)) {
					query(results, px, py, pz, node.children.get(i));
				}
			}
		}
	}

	/**
	 * Returns one item that intersects the query point, or null if no items intersect that point.
	 */
	public BoundedObject queryOne(long px, long py, long pz) {
		return queryOne(px, py, pz, root);
	}
	private BoundedObject queryOne(long px, long py, long pz, RTreeNode node) {
		if (node == null) return null;
		if (node.isLeaf()) {
			for (int i = 0; i < node.data.size(); i++) {
				if (node.data.get(i).getBounds().contains(px, py, pz)) {
					return node.data.get(i);
				}
			}
			return null;
		} else {
			for (int i = 0; i < node.children.size(); i++) {
				if (node.children.get(i).box.contains(px, py, pz)) {
					BoundedObject result = queryOne(px, py, pz, node.children.get(i));
					if (result != null) return result;
				}
			}
			return null;
		}
	}

	/**
	 * Removes the specified object if it is in the tree.
	 * @param o
	 */
	public void remove(BoundedObject o) {
		RTreeNode n = chooseLeaf(o, root);
		assert(n.isLeaf());
		n.data.remove(o);
		n.computeMBR();
	}

	/**
	 * Inserts object o into the tree. Note that if the value of o.getBounds() changes
	 * while in the R-tree, the result is undefined.
	 * @throws NullPointerException If o == null
	 */
	public void insert(RTreeViewable o) {
		if (o == null) throw new NullPointerException("Cannot store null object");
		if (root == null)
			root = new RTreeNode(true);

		RTreeNode n = chooseLeaf(o, root);
		assert(n.isLeaf());
		n.data.add(o);
		n.computeMBR();
		splitter.split(n);
	}

	/**
	 * Counts the number of items in the tree.
	 */
	public int count() {
		if (root == null) return 0;
		return count(root);
	}
	private int count(RTreeNode n) {
		assert(n != null);
		if (n.isLeaf()) {
			return n.data.size();
		} else {
			int sum = 0;
			for (int i = 0; i < n.children.size(); i++)
				sum += count(n.children.get(i));
			return sum;
		}
	}

	private RTreeNode chooseLeaf(BoundedObject o, RTreeNode n) {
		assert(n != null);
		if (n.isLeaf()) {
			return n;
		} else {
			CubeL box = o.getBounds();

			int maxOverlap = Integer.MAX_VALUE;
			RTreeNode maxnode = null;
			for (int i = 0; i < n.children.size(); i++) {
				int overlap = expansionNeeded(n.children.get(i).box, box);
				if ((overlap < maxOverlap) || (overlap == maxOverlap)
						&& ((maxnode!=null)&&(area(n.children.get(i).box) < area(maxnode.box)))) {
					maxOverlap = overlap;
					maxnode = n.children.get(i);
				}
			}
			
			if (maxnode == null) // Not sure how this could occur
				return null;
			
			return chooseLeaf(o, maxnode);
		}
	}
	
	/**
	 * Returns the amount that other will need to be expanded to fit this.
	 */
	private static int expansionNeeded(CubeL one, CubeL two) {
		int total = 0;

		if(two.lx < one.lx) total += one.lx - two.lx;
		if(two.rx > one.rx) total += two.rx - one.rx;

		if(two.ty < one.ty) total += one.ty - two.ty;
		if(two.by > one.by) total += two.by - one.by;

		if(two.iz < one.iz) total += one.iz - two.iz;
		if(two.oz > one.oz) total += two.oz - one.oz;

		return total;
	}
	
	private static long area(CubeL rect) {
		return rect.width() * rect.height() * rect.depth();
	}
}