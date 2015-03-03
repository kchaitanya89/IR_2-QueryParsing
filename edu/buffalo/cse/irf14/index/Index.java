/**
 *
 */
package edu.buffalo.cse.irf14.index;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Chaitanya
 *
 * @created-on Oct 12, 20149:03:42 PM
 *
 */
public class Index {

	Map<String, Posting> a = new TreeMap<String, Posting>();
	Map<String, Posting> b = new TreeMap<String, Posting>();
	Map<String, Posting> c = new TreeMap<String, Posting>();
	Map<String, Posting> d = new TreeMap<String, Posting>();
	Map<String, Posting> e = new TreeMap<String, Posting>();
	Map<String, Posting> f = new TreeMap<String, Posting>();
	Map<String, Posting> g = new TreeMap<String, Posting>();
	Map<String, Posting> h = new TreeMap<String, Posting>();
	Map<String, Posting> i = new TreeMap<String, Posting>();
	Map<String, Posting> j = new TreeMap<String, Posting>();
	Map<String, Posting> k = new TreeMap<String, Posting>();
	Map<String, Posting> l = new TreeMap<String, Posting>();
	Map<String, Posting> m = new TreeMap<String, Posting>();
	Map<String, Posting> n = new TreeMap<String, Posting>();
	Map<String, Posting> o = new TreeMap<String, Posting>();
	Map<String, Posting> p = new TreeMap<String, Posting>();
	Map<String, Posting> q = new TreeMap<String, Posting>();
	Map<String, Posting> r = new TreeMap<String, Posting>();
	Map<String, Posting> s = new TreeMap<String, Posting>();
	Map<String, Posting> t = new TreeMap<String, Posting>();
	Map<String, Posting> u = new TreeMap<String, Posting>();
	Map<String, Posting> v = new TreeMap<String, Posting>();
	Map<String, Posting> w = new TreeMap<String, Posting>();
	Map<String, Posting> x = new TreeMap<String, Posting>();
	Map<String, Posting> y = new TreeMap<String, Posting>();
	Map<String, Posting> z = new TreeMap<String, Posting>();
	Map<String, Posting> spl = new TreeMap<String, Posting>();

	/**
	 *
	 */
	public Posting put(String key, Posting value) {
		if (key != null && key.length() > 0) {
			char caseSearch = Character.toLowerCase(key.charAt(0));
			switch (caseSearch) {
			case 'a':
				return a.put(key, value);
			case 'b':
				return b.put(key, value);
			case 'c':
				return c.put(key, value);
			case 'd':
				return d.put(key, value);
			case 'e':
				return e.put(key, value);
			case 'f':
				return f.put(key, value);
			case 'g':
				return g.put(key, value);
			case 'h':
				return h.put(key, value);
			case 'i':
				return i.put(key, value);
			case 'j':
				return j.put(key, value);
			case 'k':
				return k.put(key, value);
			case 'l':
				return l.put(key, value);
			case 'm':
				return m.put(key, value);
			case 'n':
				return n.put(key, value);
			case 'o':
				return o.put(key, value);
			case 'p':
				return p.put(key, value);
			case 'q':
				return q.put(key, value);
			case 'r':
				return r.put(key, value);
			case 's':
				return s.put(key, value);
			case 't':
				return t.put(key, value);
			case 'u':
				return u.put(key, value);
			case 'v':
				return v.put(key, value);
			case 'w':
				return w.put(key, value);
			case 'x':
				return x.put(key, value);
			case 'y':
				return y.put(key, value);
			case 'z':
				return z.put(key, value);
			default:
				return spl.put(key, value);
			}
		}
		return null;

	}

	public Posting get(String key) {
		if (key != null && key.length() > 0) {
			char caseSearch = Character.toLowerCase(key.charAt(0));
			switch (caseSearch) {
			case 'a':
				return a.get(key);
			case 'b':
				return b.get(key);
			case 'c':
				return c.get(key);
			case 'd':
				return d.get(key);
			case 'e':
				return e.get(key);
			case 'f':
				return f.get(key);
			case 'g':
				return g.get(key);
			case 'h':
				return h.get(key);
			case 'i':
				return i.get(key);
			case 'j':
				return j.get(key);
			case 'k':
				return k.get(key);
			case 'l':
				return l.get(key);
			case 'm':
				return m.get(key);
			case 'n':
				return n.get(key);
			case 'o':
				return o.get(key);
			case 'p':
				return p.get(key);
			case 'q':
				return q.get(key);
			case 'r':
				return r.get(key);
			case 's':
				return s.get(key);
			case 't':
				return t.get(key);
			case 'u':
				return u.get(key);
			case 'v':
				return v.get(key);
			case 'w':
				return w.get(key);
			case 'x':
				return x.get(key);
			case 'y':
				return y.get(key);
			case 'z':
				return z.get(key);
			default:
				return spl.get(key);
			}
		}
		return null;
	}

	/**
	 * @return
	 */
	public int size() {
		int size = 0;
		for (Field field : this.getClass().getDeclaredFields()) {
			try {
				size += ((TreeMap<String, Posting>) field.get(this)).size();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return size;
	}

	/**
	 * @return
	 */
	public int valueSize() {
		Set<String> set = new TreeSet<String>();
		for (Field field : this.getClass().getDeclaredFields()) {
			TreeMap<String, Posting> index;
			try {
				index = (TreeMap<String, Posting>) field.get(this);
				for (Object key : index.keySet()) {

					Posting posting = index.get(key);
					Map<String, Posting.Entry> individualPostingsMap = posting
							.getIndividualPostingsMap();

					for (Object innerKey : individualPostingsMap.keySet()) {
						set.add(innerKey.toString());
					}
				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return set != null ? set.size() : -1;
	}
}
