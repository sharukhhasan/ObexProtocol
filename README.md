# ObexProtocol
## Object Exchange Bluetooth Communication Protocol

OBEX works by exchanging objects, which are used for a variety of purposes: establishing the parameters of a connection, sending and requesting data, changing the current path or the attributes of a file.

  ### Objects
  Objects are fields and headers.

  * Object Fields
    * Commands | GET, Final   | 0x83
    * Length   | Total length | 0x00 0x29
  
  * Object Headers
    * Connection ID | 1 | 0xCB 0x00 0x00 0x00 0x01
    * Name | "telecom/pb.vcf" | 0x01 0x00 0x1e 0x00 0x74 0x00 0x65 0x00 0x6c 0x00 0x65 0x00 0x63 0x00 0x6f 0x00 0x6d 0x00 0x2f 0x00 0x70 0x00 0x62 0x00 0x2e 0x00 0x76 0x00 0x63 0x00 0x66 0x00 0x00
  
  This object contains two fields (command and length) and two headers. The first field (command) specifies that is a request for data (GET). The second field is the total size of the object, including the two fields.

  This object also contains two headers, specifically a "Connection ID" and a "Name". The first byte of each header is the header's name and its content type. In this case:

  0xCB means that this header is a "Connection ID", a number obtained previously; the two highest-order bits of 0xCB are 11, and this pair specifies that this as a 4-byte quantity;
  the first byte of the second header is 0x01; this byte identifies this header as a "Name" one; the first two bits of 0x01 are 00, meaning that the content of this header is a null-terminated unicode string (in UCS-2 form), prefixed by the number of bytes it is made of (0x00 0x1e).


  ### Response
  Example response
  
  * Response Fields
    * Response code | OK, Final   | 0xA0
    * Length   | Total length | 0x00 0x35
  
  * Response Headers
    * End of body | "BEGIN:VCARD..." | 0x49 0x00 0x2F 0x42 0x45 0x47 0x49 0x4e 0x3a 0x56 0x43 0x41 0x52 0x44
    
    The only header has 0x49 as its identifier, meaning that it is an "End of Body", the last chunk of information (also the only one, in this case). The first two bits of 0x49 are 01, meaning that the content of this header is length-prefixed data: the two next bytes 0x00 0x2F tells the length of this data (in decimal, 47), the succeeding ones are the data, in this case a phonebook comprising only an empty vCard of 47 bytes.
