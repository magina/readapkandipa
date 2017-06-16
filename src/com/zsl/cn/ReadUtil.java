package com.zsl.cn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apkinfo.api.util.AXmlResourceParser;
import org.apkinfo.api.util.TypedValue;
import org.apkinfo.api.util.XmlPullParser;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;
/**
 * 
 * @author ZSL
 *
 */
public final class ReadUtil 
{
	/**
	 * 读取apk
	 * @param apkUrl
	 * @return
	 */
	public static Map<String,Object> readAPK(String apkUrl)
	{
		ZipFile zipFile;
		Map<String,Object> map = new HashMap<String, Object>();
		try {
			zipFile = new ZipFile(apkUrl);
			Enumeration<?> enumeration = zipFile.entries();
			ZipEntry zipEntry = null;
			while (enumeration.hasMoreElements()) 
			{
				zipEntry = (ZipEntry) enumeration.nextElement();
				if (zipEntry.isDirectory()) 
				{

				} 
				else 
				{
					if ("androidmanifest.xml".equals(zipEntry.getName().toLowerCase())) 
					{
						AXmlResourceParser parser = new AXmlResourceParser();
						parser.open(zipFile.getInputStream(zipEntry));
						while (true) 
						{
							int type = parser.next();
							if (type == XmlPullParser.END_DOCUMENT) 
							{
								break;
							}
							String name = parser.getName();
							if(null != name && name.toLowerCase().equals("manifest"))
							{
								for (int i = 0; i != parser.getAttributeCount(); i++) 
								{
									if ("versionName".equals(parser.getAttributeName(i))) 
									{
										String versionName = getAttributeValue(parser, i);
										if(null == versionName)
										{
											versionName = "";
										}
										map.put("versionName", versionName);
									} 
									else if ("package".equals(parser.getAttributeName(i))) 
									{
										String packageName = getAttributeValue(parser, i);
										if(null == packageName)
										{
											packageName = "";
										}
										map.put("package", packageName);
									} 
									else if("versionCode".equals(parser.getAttributeName(i)))
									{
										String versionCode = getAttributeValue(parser, i);
										if(null == versionCode)
										{
											versionCode = "";
										}
										map.put("versionCode", versionCode);
									}
								}
								break;
							}
						}
					}
					
				}
			}
			zipFile.close();
		} 
		catch (Exception e) 
		{
			map.put("code", "fail");
			map.put("error","读取apk失败");
		}
		return map;
	}
	
	private static String getAttributeValue(AXmlResourceParser parser, int index) 
	{
		int type = parser.getAttributeValueType(index);
		int data = parser.getAttributeValueData(index);
		if (type == TypedValue.TYPE_STRING) 
		{
			return parser.getAttributeValue(index);
		}
		if (type == TypedValue.TYPE_ATTRIBUTE) 
		{
			return String.format("?%s%08X", getPackage(data), data);
		}
		if (type == TypedValue.TYPE_REFERENCE) 
		{
			return String.format("@%s%08X", getPackage(data), data);
		}
		if (type == TypedValue.TYPE_FLOAT) 
		{
			return String.valueOf(Float.intBitsToFloat(data));
		}
		if (type == TypedValue.TYPE_INT_HEX) 
		{
			return String.format("0x%08X", data);
		}
		if (type == TypedValue.TYPE_INT_BOOLEAN) 
		{
			return data != 0 ? "true" : "false";
		}
		if (type == TypedValue.TYPE_DIMENSION) 
		{
			return Float.toString(complexToFloat(data)) + DIMENSION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
		}
		if (type == TypedValue.TYPE_FRACTION) 
		{
			return Float.toString(complexToFloat(data)) + FRACTION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
		}
		if (type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT) 
		{
			return String.format("#%08X", data);
		}
		if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) 
		{
			return String.valueOf(data);
		}
		return String.format("<0x%X, type 0x%02X>", data, type);
	}

	private static String getPackage(int id) 
	{
		if (id >>> 24 == 1) {
			return "android:";
		}
		return "";
	}

	// ///////////////////////////////// ILLEGAL STUFF, DONT LOOK :)
	public static float complexToFloat(int complex) 
	{
		return (float) (complex & 0xFFFFFF00) * RADIX_MULTS[(complex >> 4) & 3];
	}

	private static final float RADIX_MULTS[] = { 0.00390625F, 3.051758E-005F, 1.192093E-007F, 4.656613E-010F };
	private static final String DIMENSION_UNITS[] = { "px", "dip", "sp", "pt", "in", "mm", "", "" };
	private static final String FRACTION_UNITS[] = { "%", "%p", "", "", "", "", "", "" };
	
	/**
	 * 读取ipa
	 */
	public static Map<String,Object> readIPA(String ipaURL)
	{
		Map<String,Object> map = new HashMap<String,Object>();
		try {
			File file = new File(ipaURL);
            InputStream is = new FileInputStream(file);
            ZipInputStream zipIns = new ZipInputStream(is);
            ZipEntry ze;
            InputStream infoIs = null;
            while ((ze = zipIns.getNextEntry()) != null) 
            {
                if (!ze.isDirectory()) 
                {
                    String name = ze.getName();
                    if (null != name && name.toLowerCase().contains("info.plist")) 
                    {
                        ByteArrayOutputStream _copy = new ByteArrayOutputStream();
                        int chunk = 0;
                        byte[] data = new byte[1024];
                        while(-1!=(chunk=zipIns.read(data)))
                        {
                            _copy.write(data, 0, chunk);
                        }
                        infoIs = new ByteArrayInputStream(_copy.toByteArray());
                        break;
                    }
                }
            }
           
            NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(infoIs);
            ////////////////////////////////////////////////////////////////
            //如果想要查看有哪些key ，可以把下面注释放开
//            for (String keyName : rootDict.allKeys()) {
//				System.out.println(keyName + ":" + rootDict.get(keyName).toString());
//			  }
           
           
            // 应用包名
    		NSString parameters = (NSString) rootDict.get("CFBundleIdentifier");
    		map.put("package", parameters.toString());
    		// 应用版本名
    		parameters = (NSString) rootDict.objectForKey("CFBundleShortVersionString");
    		map.put("versionName", parameters.toString());
    		//应用版本号
    		parameters = (NSString) rootDict.get("CFBundleVersion");
    		map.put("versionCode", parameters.toString());
    		
            /////////////////////////////////////////////////
			infoIs.close();
	        is.close();
            zipIns.close();
            
        } catch (Exception e) {
        	map.put("code", "fail");
            map.put("error","读取ipa文件失败");
        }
        return map;
	}
	
	
	public static void main(String[] args) 
	{
		System.out.println("======apk=========");
		String apkUrl = "apps/shenmiaotaowang_966.apk";
		Map<String,Object> mapApk = ReadUtil.readAPK(apkUrl);
		for (String key : mapApk.keySet()) 
		{
			System.out.println(key + ":" + mapApk.get(key));
		}
		System.out.println("======ipa==========");
		String ipaUrl = "apps/150211092729.ipa";
		Map<String,Object> mapIpa = ReadUtil.readIPA(ipaUrl);
		for (String key : mapIpa.keySet()) 
		{
			System.out.println(key + ":" + mapIpa.get(key));
		}
	}
}
