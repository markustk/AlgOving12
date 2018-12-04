package Øving12;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by markusthomassenkoteng on 11.11.2018.
 */
public class LempelZivKompresjon {

    private String filInn = "";
    private String filUt = "";
    private final int MINIMUMS_ORDLENGDE = 4;
    private final int MAXIMUM_DISTANCE_BACK = 127;

    private byte[] bytesFromFile = new byte[0];
    private byte[] komprimertBuffer = new byte[0];

    //Tom konstruktør dersom man vil skrive inn innfil og utfil senere
    public LempelZivKompresjon() {

    }

    public LempelZivKompresjon(String filInn, String filUt) {
        this.filInn = filInn;
        this.filUt = filUt;
        lesFil();
    }

    private void lesFil() {
        try {
            bytesFromFile = Files.readAllBytes(Paths.get(filInn));
            komprimertBuffer = new byte[bytesFromFile.length];
            //System.out.println(bytesFromFile.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void skrivFil() {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filUt)));
            dataOutputStream.write(komprimertBuffer);
            dataOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Finner beste match til et ord, beste match er det første ordet som er like langt
    //Metoden finner posisjonen til dette ordet
    private int finnBesteMatch(ArrayList<Byte> bytesToMatch, int startIndex) {

        if (startIndex < MINIMUMS_ORDLENGDE) { //Skal egentlig aldri komme hit
            return -3;
        }
        //System.out.println(bytesToMatch.size());
        //Sjekke hvor langt en kan gå tilbake å søke etter lignenede bytes
        //MAXIMUM_DISTANCE_BACK = 127
        int startBackIndex = startIndex - MAXIMUM_DISTANCE_BACK;
        int distanceBack = MAXIMUM_DISTANCE_BACK;
        if (startBackIndex < 0) {
            startBackIndex = 0;
            distanceBack = startIndex;
        }

        for (int i = startBackIndex; i <= (distanceBack - MINIMUMS_ORDLENGDE); i++) { //Går for hver bokstav siden -127 eller så langt som mulig fra startVerdi og opp til start-verdi
            boolean isFound = true;
            for (int j = i, k = 0; j < filInn.length() && k < bytesToMatch.size(); j++, k++) { //Sjekker for hvert ord
                if (tempBlock[j] != bytesToMatch.get(k)) { //Sjekker hver bokstav i hvert ord
                    isFound = false;
                    break;
                }
            }

            if (isFound) {
                return i;
            }
        }
        return -2;
    }

    private byte[] tempBlock;
    private int startHent = 0;
    private int bytesIgjen;

    private void hentNyBlokk(int bytesIgjen) {
        if (bytesIgjen < MAXIMUM_DISTANCE_BACK) {
            tempBlock = new byte[bytesIgjen];
            for (int i = 0; i < bytesIgjen; i++, startHent++) {
                tempBlock[i] = bytesFromFile[startHent];
            }
            this.bytesIgjen = 0;
        } else {
            tempBlock = new byte[MAXIMUM_DISTANCE_BACK];
            for (int i = 0; i < MAXIMUM_DISTANCE_BACK; i++, startHent++) {
                tempBlock[i] = bytesFromFile[startHent];
            }
            this.bytesIgjen -= MAXIMUM_DISTANCE_BACK;
        }
    }

    public void komprimerFil() {

        bytesIgjen = bytesFromFile.length;
        int bufferIndex = 0;

        while (bytesIgjen != 0) {
            hentNyBlokk(bytesIgjen);

            int byteIndex = 0; //Hvor en skal sette inn neste byte [-10], [10, 10] som beskrivet i leksjon
            //String testUtskrift = ""; //For å teste

            boolean funnetKomprimering = false;
            int doneBytes = 0;
            int komprimeringsIndex = -2;

            for (int i = 0; i < bytesFromFile.length; i++) { //For hver bokstav i hele filen

                ArrayList<Byte> currentBytes = new ArrayList<Byte>();

                funnetKomprimering = false;
                int komprimeringsLengde = -2;
                int ukomprimerteBytes = -2;
                int startKomprimeringsIndex = 0;

                for (int j = i; j < bytesFromFile.length; j++) { //For i, så går den i en løkke frammover slik at vi får ulike ord: ABCDEF blir til ABC (i=0), og så BCD (i=1) osv...
                    currentBytes.add(bytesFromFile[j]);

                    //Sjekke om noen bytes kan matche disse bytesene, må ha lengre ord enn minimumslengden
                    if ((currentBytes.size() >= MINIMUMS_ORDLENGDE) && (i >= MINIMUMS_ORDLENGDE) && (bytesFromFile.length - i >= MINIMUMS_ORDLENGDE)) {

                        int komprimeringsPlass = finnBesteMatch(currentBytes, i);
                        if (komprimeringsPlass >= 0) {
                            funnetKomprimering = true;
                            komprimeringsIndex = i;
                            startKomprimeringsIndex = komprimeringsPlass;
                            komprimeringsLengde = currentBytes.size();
                        } else {
                            break;
                        }
                    }
                }

                if (funnetKomprimering) {
                    int ukomprimert = komprimeringsIndex - doneBytes; //komprimeringsIndex - lastKomprimeringsIndex
                    //testUtskrift += "[" + ukomprimert + "]";

                    komprimertBuffer[bufferIndex] = (byte) -ukomprimert;
                    //System.out.print("Bufferindex: " + bufferIndex);
                    bufferIndex++;


                    for (int b = doneBytes; b < komprimeringsIndex; b++, bufferIndex++) {
                        //testUtskrift += (char) bytesFromFile[b];
                        //System.out.print(b + ", ");
                        komprimertBuffer[bufferIndex] = bytesFromFile[b];
                    }

                    int tilbake = komprimeringsIndex - startKomprimeringsIndex;

                    //testUtskrift += "[-" + tilbake + "," + komprimeringsLengde + "]";
                    komprimertBuffer[bufferIndex] = (byte) tilbake;
                    bufferIndex++;
                    komprimertBuffer[bufferIndex] = (byte) komprimeringsLengde;
                    bufferIndex++;

                    doneBytes = komprimeringsIndex + komprimeringsLengde;
                    i += komprimeringsLengde; //= komprimeringsIndex ?

                }

            }

            int ukomprimert = bytesFromFile.length - doneBytes; //komprimeringsIndex - doneBytes; //komprimeringsIndex - lastKomprimeringsIndex
            //testUtskrift += "[" + ukomprimert + "]";

            komprimertBuffer[bufferIndex] = (byte) -ukomprimert;
            bufferIndex++;

            for (int b = doneBytes; b < bytesFromFile.length; b++, bufferIndex++) {
                //testUtskrift += (char) bytesFromFile[b];
                komprimertBuffer[bufferIndex] = bytesFromFile[b];
            }

            //System.out.println(testUtskrift);
        }



        byte[] buffer = komprimertBuffer;
        fixEmptyBufferBytes(buffer, bufferIndex);
        skrivFil();
    }

    private void fixEmptyBufferBytes(byte[] buffer, int bufferLength) {
        komprimertBuffer = new byte[bufferLength];
        for (int i = 0; i < bufferLength; i++) {
            komprimertBuffer[i] = buffer[i];
        }
    }

    public void komprimerFil(String filInn, String filUt) {
        this.filInn = filInn;
        this.filUt = filUt;
        komprimerFil();
    }

    public static void main(String[] args){
        LempelZivKompresjon lempelZivKompresjon = new LempelZivKompresjon("/Users/markusthomassenkoteng/Documents/opg12.txt", "/Users/markusthomassenkoteng/Documents/komprimertTest.txt");
        lempelZivKompresjon.komprimerFil();
    }
}
