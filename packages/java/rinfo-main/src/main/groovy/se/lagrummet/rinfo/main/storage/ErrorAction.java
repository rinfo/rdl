package se.lagrummet.rinfo.main.storage;

enum ErrorAction {
    SKIPANDHALT,
    SKIPANDCONTINUE,
    STOREANDCONTINUE,
    CONTINUEANDRETRYLATER
}
