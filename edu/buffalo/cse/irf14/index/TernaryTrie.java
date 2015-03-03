package edu.buffalo.cse.irf14.index;


public class TernaryTrie {
	
	private static class Node{
		Object val;
		char c;
		Node left, mid, right;
	}
	
	private static class RootNode{
		FirstNode next[] = new FirstNode[26];
	}
	
	private static class FirstNode{
		Object val;
		Node next;
	}
	
	private RootNode root = new RootNode();
	
	private TernaryTree other = new TernaryTree();
	
	private int size;
	
	private int maxKeyLength;
	
	private int charToInt(char c){
		c = Character.toLowerCase(c);
		int i = c-'a';
		return i<0?26:i;
	}
	
	public void put(String key, Object val){
		if(key==null || key.isEmpty()){
			return;
		}
		int i = 0;
		int l = key.length();
		maxKeyLength = l>maxKeyLength?l:maxKeyLength;
		if(i<l){
			int j = charToInt(key.charAt(i++));
			if(j==26){
				putIntoOther(key,val);
				return;
			}
			if(root.next[j] == null){
				root.next[j] = new FirstNode();
			}
			FirstNode fn = root.next[j];
			if(i<l){
				fn.next = put(fn.next, key, val, i);
			}else{
				if(fn.val==null){
					size++;
				}
				fn.val = val;
			}
		}		
	}
	
	private void putIntoOther(String key, Object val){
		other.put(key, val);
	}

	private Node put(Node x, String key, Object val, int d) {
		char c = key.charAt(d);
		if(x==null){
			x = new Node();
			x.c = c;
		}
		if(c<x.c){
			x.left = put(x.left,key,val,d);
		}else if(c>x.c){
			x.right = put(x.right,key,val,d);
		}else if(d<key.length()-1){
			x.mid = put(x.mid,key,val,d+1);
		}else{
			if(x.val==null){
				size++;
			}
			x.val = val;
		}
		return x;
	}
	
	public boolean contains(String key){
		return get(key)!=null;
	}
	
	public Object get(String key){
		if(key==null || key.isEmpty()){
			return null;
		}
		int i=0;
		int j = charToInt(key.charAt(i));
		if(j==26){
			return other.get(key);
		}		
		FirstNode fn = null;
		int lastInd = key.length()-1;
		fn = root.next[j];
		if(fn==null){
			return null;
		}
		
		if(i<lastInd){
			i++;
			Node n = fn.next;
			while(n!=null){
				char c = key.charAt(i);
				if(c<n.c){
					n = n.left;
				}else if(c == n.c){
					if(i == lastInd){
						return n.val;
					}
					n = n.mid;
					i++;
				}else{
					n = n.right;
				}
			}
		}else{
			return fn.val;
		}
		return null;
	}
	
	public int size(){
		return size+other.size();
	}
	
	public int maxKeyLength(){
		return maxKeyLength;
	}

	public static void main(String[] args) {
		String arr[] = {"are","By","she","shell","shore","sea","surely","the","temptation","she","123a"};
		TernaryTrie tst = new TernaryTrie();
		tst.put("ashok", 0);
		tst.put("shell", -1);
		System.out.println(tst.get("shell"));
		int i=1;
		for(String key:arr){
			tst.put(key, i++);
		}
		System.out.println(tst.size()+" "+tst.maxKeyLength());
		for(String key:arr){
			System.out.print(tst.get(key)+" ");
		}
		System.out.println();
		String[] arr1 = {"area","a123","shells","She","SHE","Temptation","223a"};
		for(String key:arr1){
			System.out.println(tst.get(key));
		}
	}

}
