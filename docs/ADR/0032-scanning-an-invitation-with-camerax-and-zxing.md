# ADR-0032: An invitation is scanned with CameraX and zxing

- Status: Accepted
- Date: 2026-10-05

## Context

An administrator shows an invitation as a QR code on their own screen
(ADR-0019). Until now the person joining could only open or paste the link,
which means it had to be sent to them first. A scanner on the connection
screen lets them read it off the administrator's phone instead.

The application runs on phones without Google Play Services. Three ways to
read a QR code from the camera were weighed:

- ML Kit through Play Services: the model is downloaded by Play Services, so
  it does not work on such phones at all;
- ML Kit with the model bundled: works without Play Services and adds about
  2.4 MB to the package, but it is a closed library of Google's;
- CameraX for the camera, and zxing for reading the frames. zxing core is
  already in the application, where it draws the invitation's QR code.

## Decision

- The camera is CameraX (`camera-camera2`, `camera-lifecycle`,
  `camera-view`): a preview to aim with and an image analysis stream that
  keeps only the newest frame.
- Each frame's brightness plane is read by zxing core's QR reader. The whole
  frame is searched; the square on the screen only shows where to aim.
- A code that is a connection link goes where a pasted link goes. Any other
  code is reported for three seconds, and scanning goes on.
- On the connection screen the scanner is the first button and pasting the
  second. In the settings, an icon in the invitation field opens it and
  puts the link it read into the field.
- The camera permission is asked for when the scanner is opened, never
  before. Once the system stops asking, the scanner sends the person to the
  application's settings.
- A read invitation gives a short vibration.

## Consequences

- The scanner works on every phone the rest of the application works on,
  with no closed library added.
- The package grows by CameraX's size; zxing was already in it.
- zxing reads a sharp, well-lit code reliably but is slower than ML Kit on
  a blurred or angled one. Holding the phone still in front of the other
  screen is enough.
- The application now asks for the camera and for vibration. A phone
  without a camera can still install it: the camera is declared as not
  required, and the person pastes the link instead.
