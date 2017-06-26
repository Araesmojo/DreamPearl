package com.araes.rimagenext;
import java.lang.reflect.Array;

public class GameUtils
{
	GameUtils(){
		// do nothing
	}
	
	public static float[] ConcatFloatArrays( float[] arrA, float[] arrB ){
		int aLen = arrA.length;
		int bLen = arrB.length;
		float[] arrOut= new float[aLen+bLen];
		System.arraycopy(arrA, 0, arrOut, 0, aLen);
		System.arraycopy(arrB, 0, arrOut, aLen, bLen);
		return arrOut;
	}
	
	public static String[] ConcatStringArrays( String[] arrA, String[] arrB ){
		int aLen = arrA.length;
		int bLen = arrB.length;
		String[] arrOut= new String[aLen+bLen];
		System.arraycopy(arrA, 0, arrOut, 0, aLen);
		System.arraycopy(arrB, 0, arrOut, aLen, bLen);
		return arrOut;
	}
	
	public static <T> T[] ConcatObjArrays (T[] a, T[] b) {
		int aLen = a.length;
		int bLen = b.length;

		@SuppressWarnings("unchecked")
			T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen+bLen);
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);

		return c;
	}
	
	public static String MatToS( float[] mat ){
		String out = "";
		for( int i = 0; i < mat.length-1; i++ ){
			out += Float.toString(mat[i]) + ", ";
		}
		out += Float.toString(mat[mat.length-1]);
		return out;
	}
}
