# ADR-0030: Names for people and phones, for oneself alone

- Status: Accepted
- Date: 2026-10-05

## Context

A phone is named by its maker's model (ADR-0007) and a person by the name on
their account. In a household that reads "MAR-LX1M" and "Vitaly O.", where
the family says "the new phone" and "grandpa". A name the owner gives a phone
is one name for everybody; what each viewer calls things differs.

## Decision

- Each member keeps a document of their own, `labels/{uid}`: names for
  phones, keyed by the phone's full id (account and phone joined by a dot,
  so two phones of one model are two names), and names for members.
- Only that member reads and writes it; the person named does not see it.
- At most 500 phone names and 50 member names, each cut to 40 characters on
  the phone and on the page alike.
- A phone is shown by the viewer's name for it, else the name its owner gave
  it, else its model; a person by the viewer's name for them, else the name
  in their profile.

## Consequences

- Nobody learns what others call them.
- The names follow the member to every device and to the page, being in the
  group's database; a member in two groups names things twice.
