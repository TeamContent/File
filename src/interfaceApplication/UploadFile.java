package interfaceApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.simple.JSONObject;

import esayhelper.JSONHelper;
import esayhelper.TimeHelper;
import esayhelper.jGrapeFW_Message;
import model.OpFile;
@WebServlet(name = "UploadFile", urlPatterns = { "/UploadFile" })
public class UploadFile extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private OpFile files = new OpFile();
	private String path = "";
	private String fileName = "";
	private String fatherid = "";
	private String MD5 = "";

	private String ExtName = ""; // 扩展名

	public UploadFile() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Content-type", "text/html;charset=UTF-8");
		String appid = request.getParameter("appid"); // 分表字段
		fatherid = request.getParameter("folderid");
		boolean uploadDone = true;
		try {
			String Date = TimeHelper.stampToDate(TimeHelper.nowMillis()).split(" ")[0];
			boolean isMultipart = ServletFileUpload.isMultipartContent(request);
			if (isMultipart) {
				FileItemFactory factory = new DiskFileItemFactory();
				path = this.getServletContext().getRealPath("/upload/" + Date);
				if (!new File(path).exists()) {
					new File(path).mkdir();
				}
				ServletFileUpload upload = new ServletFileUpload(factory);
				// 得到所有的表单域，它们目前都被当作FileItem
				List<FileItem> fileItems = upload.parseRequest(request);
				String id = "";
				// 如果大于1说明是分片处理
				int chunks = 1;
				int chunk = 0;
				long filesize = 0;
				FileItem tempFileItem = null;
				if (fileItems != null && fileItems.size() > 0) {
					for (FileItem fileItem : fileItems) {
						if (!fileItem.isFormField()) {
							fileName = fileItem.getName();
							String string = getJson(appid, fileName, String.valueOf(fileItem.getSize()), fatherid);
							if (string != null && !("").equals(string)) {
								fileItem.write(new File(path + "\\" + fileName));
								response.getWriter().println(string);
								return;
							}
						}
						if (fileItem.getFieldName().equals("id")) {
							id = fileItem.getString();
						} else if (fileItem.getFieldName().equals("name")) {
							fileName = new String(fileItem.getString().getBytes("ISO-8859-1"), "UTF-8");
							MD5 = DigestUtils.md5Hex(path + "\\" + fileName);
							if (files.search(appid, fileName)) {
								response.getWriter().print(jGrapeFW_Message.netMSG(0, "文件已存在"));
								return;
							}
						} else if (fileItem.getFieldName().equals("chunks")) {
							chunks = NumberUtils.toInt(fileItem.getString());
						} else if (fileItem.getFieldName().equals("chunk")) {
							chunk = NumberUtils.toInt(fileItem.getString());
						} else if (fileItem.getFieldName().equals("file")) {
							tempFileItem = fileItem;
						}
						filesize += fileItem.getSize();
					}
				}
				String tempFileDir = getTempFilePath(path) + File.separator + id;
				File parentFileDir = new File(tempFileDir);
				if (!parentFileDir.exists()) {
					parentFileDir.mkdirs();
				}
				// 分片处理时，前台会多次调用上传接口，每次都会上传文件的一部分到后台(默认每片为5M)
				File tempPartFile = new File(parentFileDir, fileName + "_" + chunk + ".part");
				FileUtils.copyInputStreamToFile(tempFileItem.getInputStream(), tempPartFile);
				// 是否全部上传完成
				// 所有分片都存在才说明整个文件上传完成
				for (int i = 0; i < chunks; i++) {
					File partFile = new File(parentFileDir, fileName + "_" + i + ".part");
					if (!partFile.exists()) {
						uploadDone = false;
					}
				}
				if (uploadDone) {
					File destTempFile = new File(path, fileName);
					for (int i = 0; i < chunks; i++) {
						File partFile = new File(parentFileDir, fileName + "_" + i + ".part");
						FileOutputStream destTempfos = new FileOutputStream(destTempFile, true);
						System.out.println(destTempfos.toString());
						FileUtils.copyFile(partFile, destTempfos);
						destTempfos.close();
					}
					FileUtils.deleteDirectory(parentFileDir);
					String mString = getJson(appid, fileName, String.valueOf(filesize), fatherid);
					if (mString != null && !("").equals(mString)) {
						response.getWriter().print(mString);
					}
					// JSONObject object = new JSONObject();
					// object.put("fileoldname", fileName);
					// object.put("filenewname", mknew(fileName));
					// object.put("filetype", GetFileType(ExtName));
					// object.put("fileextname", ext(fileName));
					// object.put("size", String.valueOf(filesize));
					// object.put("fatherid", fatherid);
					// object.put("filepath", path.split("webapps")[1] + "\\" +
					// fileName);
					// // object.put("videoimg", filetype == 2
					// // ? VideoImg.split("webapps")[1] : VideoImg);
					// // object.put("videoimg","");
					// object.put("isdelete", 0);
					// object.put("MD5", MD5);
				} else {
					if (chunk == chunks - 1) {
						FileUtils.deleteDirectory(parentFileDir);
					}
					response.getWriter().print("文件上传失败");
				}
			}
		} catch (Exception e) {
			response.getWriter().print("文件上传失败");
		}
	}

	private String getTempFilePath(String tempath) {
		File file = new File(tempath);
		if (!file.exists()) {
			file.mkdirs();
		}
		return tempath;
	}

	// 新文件名称
	public String mknew(String name) {
		String str = UUID.randomUUID().toString();
		String names = ext(name);
		return !names.equals(".") ? (str.replace("-", "") + "." + names) : (str.replace("-", ""));
	}

	// 获取扩展名
	private String ext(String name) {
		if (name.contains(".")) {
			ExtName = name.substring(name.lastIndexOf(".") + 1);
		} else {
			ExtName = "";
		}
		return ExtName;
	}

	@SuppressWarnings("unchecked")
	private String getJson(String appid, String filename, String filesize, String fatherid) {
		MD5 = DigestUtils.md5Hex(path + "\\" + fileName);
		if (files.search(appid, fileName)) {
			return jGrapeFW_Message.netMSG(0, "文件已存在");
		}
		JSONObject object = new JSONObject();
		object.put("fileoldname", fileName);
		object.put("filenewname", mknew(fileName));
		object.put("filetype", GetFileType(ExtName));
		object.put("fileextname", ext(fileName));
		object.put("size", String.valueOf(filesize));
		object.put("fatherid", fatherid);
		object.put("filepath", path.split("webapps")[1] + "\\" + fileName);
		String string = files.insert(appid, object);
		if (string != null && !("").equals(string)) {
			return string;
		}
		return "";
	}

	// 判断文件类型
	public int GetFileType(String extname) {
		int type;
		switch (extname.toLowerCase()) {
		// 图片
		case "png":
		case "jpg":
		case "gif":
		case "jpeg":
		case "tiff":
		case "raw":
		case "bmp":
			type = 1;
			break;
		// 视频
		case "avi":
		case "rmvb":
		case "rm":
		case "mkv":
		case "mp4":
		case "wmv":
		case "ogg":
		case "mov":
			type = 2;
			// filetype = 2;
			// VideoImg = CutPic(type);
			break;
		// 文档
		case "doc":
		case "docx":
		case "wps":
		case "xls":
		case "xlxs":
		case "ppt":
		case "txt":
		case "htm":
		case "html":
		case "pdf":
		case "dwg":
		case "exe":
			type = 3;
			break;
		// 音频
		case "mp3":
		case "wav":
		case "wma":
			type = 4;
			break;
		// 其他
		default:
			type = 5;
			break;
		}
		return type;
	}

	/**
	 * 截取指定时间的视频图片
	 * 
	 * @param path
	 * @param outpath
	 */
	/*
	 * private String CutPic(int type) { if (type != 2) { return ""; } else {
	 * String outpath = path + "\\" + "thumbnail"; if (!new
	 * File(outpath).exists()) { new File(outpath).mkdir(); } outpath = outpath
	 * + "\\" + fileName.substring(0, fileName.lastIndexOf(".")) + ".jpg";
	 * List<String> commend = new ArrayList<String>();
	 * commend.add("c:\\ffmpegtest\\ffmpeg.exe"); commend.add("-i");
	 * commend.add(path + "\\" + fileName); commend.add("-y");
	 * commend.add("-f"); commend.add("image2"); commend.add("-ss");
	 * commend.add("2"); // 截取多少秒之后的图片 commend.add("-t"); commend.add("0.001");
	 * commend.add("-s"); commend.add("350*240"); commend.add(outpath); try {
	 * ProcessBuilder builder = new ProcessBuilder(); builder.command(commend);
	 * builder.redirectErrorStream(true); Process process = builder.start(); }
	 * catch (Exception e) { e.printStackTrace(); } return outpath; } }
	 */
}
