package com.example.usermanagement.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problemDetail.setTitle("User Not Found");
        problemDetail.setDetail(ex.getMessage());

        return problemDetail;
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(
            EmailAlreadyExistsException ex) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.CONFLICT);

        problemDetail.setTitle("Email Already Exists");
        problemDetail.setDetail(ex.getMessage());

        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problemDetail.setTitle("Validation Failed");
        problemDetail.setDetail("One or more fields are invalid.");

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGenericException(Exception ex) {

		log.error("Unexpected error while processing request", ex);

		ProblemDetail problemDetail =
				ProblemDetail.forStatus(
						HttpStatus.INTERNAL_SERVER_ERROR
				);

		problemDetail.setTitle("Internal Server Error");
		problemDetail.setDetail(
				"An unexpected error occurred."
		);

		return problemDetail;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleMalformedJson(
			HttpMessageNotReadableException ex) {

		ProblemDetail problemDetail =
				ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

		problemDetail.setTitle("Malformed JSON");
		problemDetail.setDetail(
				"The request body contains invalid JSON."
		);

		return problemDetail;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(
			MethodArgumentTypeMismatchException ex) {

		ProblemDetail problemDetail =
				ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

		problemDetail.setTitle("Invalid Parameter");
		problemDetail.setDetail(
				"Invalid value for parameter: " + ex.getName()
		);

		return problemDetail;
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ProblemDetail handleMissingParameter(
			MissingServletRequestParameterException ex) {

		ProblemDetail problemDetail =
				ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

		problemDetail.setTitle("Missing Parameter");
		problemDetail.setDetail(
				"Required parameter is missing: " + ex.getParameterName()
		);

		return problemDetail;
	}
}

/*
Yes — now we can see the exact issue.
Your LoginRequest validation is fine. The problem is that your login() method uses generic RuntimeException for authentication failures, while your GlobalExceptionHandler treats every unrecognized exception as a 500 Internal Server Error.
You should introduce dedicated exceptions for the authentication cases.
1. Create InvalidCredentialsException
Put it in:
com.example.usermanagement.exception

package com.example.usermanagement.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}

2. Create UserAccountDisabledException
package com.example.usermanagement.exception;

public class UserAccountDisabledException extends RuntimeException {

    public UserAccountDisabledException() {
        super("User account is disabled");
    }
}

3. Change AuthService.login()
Your method becomes:
public LoginResponse login(LoginRequest request) {

    User user = userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(InvalidCredentialsException::new);

    if (!user.isEnabled()) {
        throw new UserAccountDisabledException();
    }

    boolean passwordMatches =
            passwordEncoder.matches(
                    request.getPassword(),
                    user.getPassword()
            );

    if (!passwordMatches) {
        throw new InvalidCredentialsException();
    }

    String accessToken = jwtService.generateToken(user);

    return new LoginResponse(accessToken);
}

Notice the important change:
.orElseThrow(InvalidCredentialsException::new);

instead of:
.orElseThrow(() ->
    new RuntimeException("Invalid email or password")
);

And:
throw new InvalidCredentialsException();

instead of:
throw new RuntimeException("Invalid email or password");

4. Add handlers to GlobalExceptionHandler
Add these before your generic Exception handler:
@ExceptionHandler(InvalidCredentialsException.class)
public ProblemDetail handleInvalidCredentials(
        InvalidCredentialsException ex) {

    ProblemDetail problemDetail =
            ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);

    problemDetail.setTitle("Invalid Credentials");
    problemDetail.setDetail(ex.getMessage());

    return problemDetail;
}

@ExceptionHandler(UserAccountDisabledException.class)
public ProblemDetail handleUserAccountDisabled(
        UserAccountDisabledException ex) {

    ProblemDetail problemDetail =
            ProblemDetail.forStatus(HttpStatus.FORBIDDEN);

    problemDetail.setTitle("Account Disabled");
    problemDetail.setDetail(ex.getMessage());

    return problemDetail;
}

So your exception mapping now becomes:
Situation	Exception	HTTP
User doesn't exist during login	InvalidCredentialsException	401
Wrong password	InvalidCredentialsException	401
Account disabled	UserAccountDisabledException	403
Invalid email format	MethodArgumentNotValidException	400
Missing required field	MethodArgumentNotValidException	400
Unexpected programming/server error	generic Exception	500


Why user@gmail.com gave 500
With your exact request:
{
  "email": "user@gmail.com",
  "password": "string"
}

Spring first runs:
@Email
@NotBlank
@Size

Everything passes.
Then:
authService.login(request);

runs.
If user@gmail.com doesn't exist:
userRepository.findByEmail(...)

returns an empty Optional.
Then your code executes:
.orElseThrow(() ->
    new RuntimeException("Invalid email or password")
);

Your handler has:
@ExceptionHandler(Exception.class)

so it catches that RuntimeException.
And your generic handler explicitly says:
ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);

Therefore:
RuntimeException
      ↓
Exception.class handler
      ↓
500

That's exactly why you saw:
Resolved [java.lang.RuntimeException: Invalid email or password]

in the log.
One security detail worth keeping
I would keep the same message:
Invalid email or password

for both:
- email doesn't exist
- password doesn't match
Don't return:
User with this email does not exist

because that allows someone to determine which email addresses have accounts.
So this is good:
{
  "title": "Invalid Credentials",
  "status": 401,
  "detail": "Invalid email or password"
}

for both cases.
And your validation test should now be separated
Test 1 — malformed email
{
  "email": "user",
  "password": "string"
}

Expected:
400 Bad Request
Validation Failed

with:
"errors": {
  "email": "Email should be valid"
}

Test 2 — valid email, nonexistent user
{
  "email": "doesnotexist@gmail.com",
  "password": "string"
}

Expected:
401 Unauthorized
Invalid Credentials

Test 3 — valid user, wrong password
{
  "email": "existing-user@gmail.com",
  "password": "wrong-password"
}

Expected:
401 Unauthorized
Invalid Credentials

Test 4 — disabled account
Expected:
403 Forbidden
Account Disabled

Test 5 — valid credentials
Expected:
200 OK

with your:
{
  "accessToken": "..."
}

This is the clean separation you want: Bean Validation handles malformed input; authentication logic handles authentication failures; the generic handler is reserved for genuinely unexpected server errors.
*/