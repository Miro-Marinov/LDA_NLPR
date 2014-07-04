import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class XMLparser {
    List<File> files = new ArrayList<>();

	public void parse(List<File> files) {		

		for (File file: files) {
			parse(file);
		}

	}
	
	public void parse(File file) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		// get the fileName without the extension
		String fileName = file.getName().substring(file.getName().lastIndexOf("/") + 1, file.getName().lastIndexOf("."));
		File outFile = new File("SemiEval2010 txt/" + fileName + ".txt");
		String targetWord = fileName.substring(0, fileName.indexOf("."));
		ContextList contextList = new ContextList(targetWord);
		
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();
			//replace file if exists
			if (outFile.exists()) {
				outFile.delete();
 			}
			outFile.createNewFile();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
			System.out.println("\nCurrent URI :" + doc.getDocumentURI());
			
			NodeList rootList = doc.getChildNodes();
			
			for (int root = 0; root < rootList.getLength(); root ++) {
				
				Node rNode = rootList.item(root);
				NodeList childList = rNode.getChildNodes();
				
				for (int child = 0; child < childList.getLength(); child ++) {
					Node cNode = childList.item(child);
					contextList.contexts.add(cNode.getTextContent());	
					
					bw.write(cNode.getTextContent());
					bw.newLine();
					bw.flush();
				}
			}
			bw.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void getFileNamesInFolder(final File folder) {
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	getFileNamesInFolder(fileEntry);
	        } else {
	            files.add(fileEntry);
	        }
	    }
	}
	

	public static void main(String[] args){
		XMLparser parser = new XMLparser();
		//parser.getFileNamesInFolder(new File("SemiEval2010 xml"));
		
		parser.files = new ArrayList<>();
		parser.getFileNamesInFolder(new File("SemiEval2010 xml"));
		for(File file: parser.files)
			parser.parse(file);	
		
	}
}
