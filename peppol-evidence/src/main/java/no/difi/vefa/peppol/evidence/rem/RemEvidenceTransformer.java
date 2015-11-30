package no.difi.vefa.peppol.evidence.rem;

import org.etsi.uri._02640.v2_.REMEvidenceType;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Transforms SignedRemEvidence back and forth between various representations like for instance
 * W3C Document and XML.
 *
 * <p>
 * The constructor is package protected as you are expected to use the {@link RemEvidenceService}  to
 * create instances of this class.
 * <p>
 * Created by steinar on 08.11.2015.
 */
class RemEvidenceTransformer {


    private JAXBContext jaxbContext;
    private boolean formattedOutput = true;

    RemEvidenceTransformer(JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
    }

    /**
     * Transforms SignedRemEvidence into XML representation suitable for signature verification etc.
     * I.e. the output is not formatted.
     *
     * @param signedRemEvidence instance to be formatted.
     */
    public void toUnformattedXml(SignedRemEvidence signedRemEvidence, OutputStream outputStream) {
        format(signedRemEvidence, outputStream, false);
    }

    /**
     * Transforms the supplied signed REM Evidence into it's XML representation.
     * <p>
     * NOTE! Do not use this XML representation for signature validation as this will fail.
     *
     * @param signedRemEvidence
     * @param outputStream
     */
    public void toFormattedXml(SignedRemEvidence signedRemEvidence, OutputStream outputStream) {
        format(signedRemEvidence, outputStream, true);
    }

    /**
     * Internal convenience method
     * @param signedRemEvidence rem evidence to transform
     * @param outputStream into which the formatted output should be emitted.
     * @param formatted indicates whether the output should be formatted (true) or not (false)
     */
    protected void format(SignedRemEvidence signedRemEvidence, OutputStream outputStream, boolean formatted) {
        Transformer transformer = null;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException("Unable to crate a new transformer");
        }

        if (formatted) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        }
        StreamResult result = new StreamResult(outputStream);
        DOMSource source = new DOMSource(signedRemEvidence.getDocument());
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new IllegalStateException("Transformation of SignedRemEvidence to XML failed:" + e.getMessage(), e);
        }
    }

    /**
     * Parses a REM evidence instance represented as a W3C Document and creates the equivalent JAXB representation.
     * It is package protected as this is not something that should not be done outside of this package.
     *
     * @param signedRemDocument
     * @param jaxbContext
     * @return
     */
    protected static JAXBElement<REMEvidenceType> toJaxb(Document signedRemDocument, JAXBContext jaxbContext) {
        JAXBElement<REMEvidenceType> remEvidenceTypeJAXBElement;
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            remEvidenceTypeJAXBElement = unmarshaller.unmarshal(signedRemDocument, REMEvidenceType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create unmarshaller");
        }
        return remEvidenceTypeJAXBElement;
    }


    /**
     * Parses the contents of an InputStream, which is expected to supply
     * a signed REMEvidenceType in XML representation.
     *
     * Step 1: parses xml into W3C Document
     * Step 2: converts W3C Document into JAXBElement
     *
     * @param inputStream holding the xml representation of a signed REM evidence.
     * @return
     */
    public SignedRemEvidence parse(InputStream inputStream) {

        // 1) Parses XML into W3C Document
        Document parsedDocument;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            parsedDocument = documentBuilder.parse(inputStream);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to create DocumentBuilder " + e.getMessage(), e);
        } catch (SAXException | IOException e) {
            throw new IllegalStateException("Unable to parse xml input " + e.getMessage(), e);
        }

        // 2) Parses it into the JAXB representation.
        JAXBElement<REMEvidenceType> remEvidenceTypeJAXBElement = toJaxb(parsedDocument, jaxbContext);

        return new SignedRemEvidence(remEvidenceTypeJAXBElement, parsedDocument);
    }

    public boolean isFormattedOutput() {
        return formattedOutput;
    }

    public void setFormattedOutput(boolean formattedOutput) {
        this.formattedOutput = formattedOutput;
    }
}
