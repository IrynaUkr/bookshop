

## Multithreaded Book Transfer Service

#### Objective:

##### 1. Transfer books from stock to a customer
   Create a multithreaded service to transfer books from stock to a customer while maintaining the stock in a correct
   state.
   Use synchronisation mechanisms to prevent race conditions.

Use Case:

- A customer can purchase one or more books.
- Stock must be updated accurately for concurrent purchase requests.
- If stock is insufficient, the purchase must fail.
- All transactions should be thread-safe to handle multiple customers purchasing books simultaneously.

##### 2. Fetch Book Recommendations in Parallel
  When a customer purchases a book, recommend related books. Fetching recommendations from a database
  or an external service might take time, so running this operation asynchronously improves performance.
  - Customer places an order →  create an order in the database and update stock.
  - Fetch book recommendations asynchronously while processing the order.
  - Show recommendations before final checkout, allowing the customer to modify the order.
  - If the customer updates the order, modify the existing order instead of creating a new one.





_______
## Authentication

A bearer token is a type of token that's used for authorization and authentication.
JWTs are JSON-based security tokens that contain a user's identity and access rights.
When a user logs in, an authentication server generates a JWT,
which is then sent as a bearer token in the authorization header of every API request

JSON Web Tokens (JWT) is the standard for securing a stateless application.
The Spring Security framework provides methods of integrating JWT to secure REST APIs.
One of the key processes of generating a token is applying a signature to guarantee authenticity.

The jjwt-api, jjwt-impl and jjwt-jackson dependencies provide an API to generate
and sign a JWT and integrate it into Spring Security.

` class AuthEntryPointJwt implements AuthenticationEntryPoint `,

- handles authorized access attempts in a Spring Security application using JWT authentication.
  It acts as a gatekeeper, ensuring only users with valid access can access protected resources.

`class JwtAuthTokenFilter extends OncePerRequestFilter`

- intercepts incoming requests, validates JWT tokens, and authenticates users if a valid token is present

` class JWT Generator `

- The JJWT library provides a *signWith()* method to help sign a JWT with a specific cryptographic algorithm and a
  secret key.
  This signing process is essential for ensuring the integrity and authenticity of the JWT.

The signWith() method accepts the Key or SecretKey instances and signature algorithm as arguments.
The Hash-based Message Authentication Code (HMAC) algorithm is among the most commonly used signing algorithms.
Importantly, the method requires a secret key, typically a byte array, for the signing process.
We can use the Key or SecretKey instance to convert a secret string to a secret key.
Notably, we can pass an ordinary string as a secret key.
However, this lacks the security guarantees and randomness of cryptographic Key or SecretKey instances.

Using the SecretKey instance ensures the integrity and authenticity of JWT.