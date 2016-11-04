package co.megachips.hybridgpsmonitor;
import java.nio.ByteBuffer;

/**
 * 
 */

/**
 * @author nitta.yuki
 *
 */
public class CombineByte {
	/**@brief Combine 4byte data as float data.
	 *
	 * @param[in] b1 combine target data(byte)
	 * @param[in] b2 combine target data(byte)
	 * @param[in] b3 combine target data(byte)
	 * @param[in] b4 combine target data(byte)
	*/
	public static float toFloat(byte b1, byte b2, byte b3, byte b4){

		byte[] bArray = new byte[4];
		bArray[0] = b1;
		bArray[1] = b2;
		bArray[2] = b3;
		bArray[3] = b4;
		ByteBuffer buffer = ByteBuffer.wrap(bArray);

		return buffer.getFloat();
	}

	/**@brief Combine 2byte data as short data.
	 *
	 * @param[in] b1 combine target data(byte)
	 * @param[in] b2 combine target data(byte)
	*/
	public static short toShort(byte b1, byte b2){

		byte[] bArray = new byte[2];
		bArray[0] = b1;
		bArray[1] = b2;
		ByteBuffer buffer = ByteBuffer.wrap(bArray);

		return buffer.getShort();
	}


	/**@brief Combine 4byte data as int data.
	 *
	 * @param[in] b1 combine target data(byte)
	 * @param[in] b2 combine target data(byte)
	 * @param[in] b3 combine target data(byte)
	 * @param[in] b4 combine target data(byte)
	*/
	public static int toInt(byte b1, byte b2, byte b3, byte b4){

		byte[] bArray = new byte[4];
		bArray[0] = b1;
		bArray[1] = b2;
		bArray[2] = b3;
		bArray[3] = b4;
		ByteBuffer buffer = ByteBuffer.wrap(bArray);

		return buffer.getInt();
	}

	/**@brief Combine 4byte data as long data.
	 *
	 * @param[in] b1 combine target data(byte)
	 * @param[in] b2 combine target data(byte)
	 * @param[in] b3 combine target data(byte)
	 * @param[in] b4 combine target data(byte)
	*/
	public static long toLong(byte b1, byte b2, byte b3, byte b4){
		int t1 = ((int)b1<<24)    & 0xFF000000;
		int t2 = ((int)b2<<16)    & 0x00FF0000;
		int t3 = ((int)b3<<8)     & 0x0000FF00;
		int t4 = ((int)b4)        & 0x000000FF;
		long temp = (t1|t2|t3|t4) & (0xFFFFFFFFL);
		return temp;
	}

}
