package edu.buffalo.cse.irf14.index;


public class TernaryTree {

	private static class Node {
		Object val;
		char c;
		Node left, mid, right;
	}

	private Node root;

	private int size;

	private int maxKeyLength;

	public void put(String key, Object val) {
		if (key == null || key.isEmpty()) {
			return;
		}
		int len = key.length();
		maxKeyLength = len > maxKeyLength ? len : maxKeyLength;
		root = put(root, key, val, 0);
	}

	private Node put(Node x, String key, Object val, int d) {
		char c = key.charAt(d);
		if (x == null) {
			x = new Node();
			x.c = c;
		}
		if (c < x.c) {
			x.left = put(x.left, key, val, d);
		} else if (c > x.c) {
			x.right = put(x.right, key, val, d);
		} else if (d < key.length() - 1) {
			x.mid = put(x.mid, key, val, d + 1);
		} else {
			if (x.val == null) {
				size++;
			}
			x.val = val;
		}
		return x;
	}

	public boolean contains(String key) {
		return get(key) != null;
	}

	public Object get(String key) {
		if(key==null || key.isEmpty()){
			return null;
		}
		Node x = get(root, key, 0);
		if (x == null)
			return null;
		return x.val;
	}

	private Node get(Node x, String key, int d) {
		if (x == null)
			return null;
		char c = key.charAt(d);
		if (c < x.c)
			return get(x.left, key, d);
		else if (c > x.c)
			return get(x.right, key, d);
		else if (d < key.length() - 1)
			return get(x.mid, key, d + 1);
		else
			return x;
	}

	public int size() {
		return size;
	}

}
