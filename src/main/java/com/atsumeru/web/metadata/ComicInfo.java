package com.atsumeru.web.metadata;

import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

public class ComicInfo {

    public static boolean readComicInfo(BookArchive bookArchive, InputStream comicInfoStream) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(comicInfoStream);
            NodeList root = document.getElementsByTagName("ComicInfo");

            String year = null;
            String month = null;

            for (int i = 0; i < root.getLength(); i++) {
                NodeList nodes = root.item(i).getChildNodes();
                for (int j = 0; j < nodes.getLength(); j++) {
                    Node node = nodes.item(j);
                    switch (node.getNodeName()) {
                        case "Title":
                            bookArchive.setTitle(node.getTextContent());
                            break;
                        case "Circles":
                            try {
                                bookArchive.setCover(node.getTextContent());
                            } catch (Exception ex) {
                                System.err.println("Unable to parse Circles from ComicInfo.xml");
                            }
                            break;
                        case "Summary":
                            bookArchive.setDescription(node.getTextContent());
                            break;
                        case "Volume":
                            try {
                                bookArchive.setVolume(Float.parseFloat(node.getTextContent()));
                            } catch (NumberFormatException ex) {
                                System.err.println("Unable to parse volume number from ComicInfo.xml");
                            }
                            break;
                        case "Year":
                            year = node.getTextContent();
                            break;
                        case "Month":
                            month = node.getTextContent();
                            break;
                        case "Writer":
                            bookArchive.setAuthors(node.getTextContent());
                            break;
                        case "Publisher":
                            bookArchive.setPublisher(node.getTextContent());
                            break;
                        case "Genre":
                            bookArchive.setGenres(node.getTextContent());
                            break;
                        case "Characters":
                            bookArchive.setCharacters(node.getTextContent());
                            break;
                    }
                }
            }

            bookArchive.setYear(StringUtils.isNotEmpty(month) ? year + "-" + month + "-01" : year);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
