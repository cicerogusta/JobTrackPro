const { readFileSync } = require('node:fs');
const { before, after, beforeEach, test } = require('node:test');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { doc, getDoc, setDoc, deleteDoc, updateDoc } = require('firebase/firestore');
let environment;
const application = {
    userId: 'alice', company: 'Example', role: 'Android Developer', location: 'Remote',
    workMode: 'REMOTE', appliedOn: '2026-01-01', status: 'SAVED', jobUrl: 'https://example.com/job',
    notes: '', createdAt: 100, updatedAt: 100, deleted: false, revision: '00000000-0000-0000-0000-000000000001',
};
before(async () => { environment = await initializeTestEnvironment({
    projectId: 'demo-jobtrackpro', firestore: { rules: readFileSync('firestore.rules', 'utf8') },
}); });
after(async () => { await environment?.cleanup(); });
beforeEach(async () => { await environment.clearFirestore(); });
const reference = (user, owner = 'alice') => doc(
    user ? environment.authenticatedContext(user).firestore() : environment.unauthenticatedContext().firestore(),
    'users', owner, 'applications', 'application-1');
test('owner can create and read', async () => {
    await assertSucceeds(setDoc(reference('alice'), application));
    await assertSucceeds(getDoc(reference('alice')));
});
test('other accounts cannot read or write', async () => {
    await assertSucceeds(setDoc(reference('alice'), application));
    await assertFails(getDoc(reference('bob')));
    await assertFails(setDoc(reference('bob'), application));
});
test('signed out clients cannot access documents', async () => {
    await assertFails(getDoc(reference(null)));
    await assertFails(setDoc(reference(null), application));
});
test('schema, owner, status, URL and extra fields are validated', async () => {
    for (const invalid of [{userId:'bob'}, {status:'INVALID'}, {company:''}, {jobUrl:'http://example.com'}, {password:'secret'}]) {
        await assertFails(setDoc(reference('alice'), { ...application, ...invalid }));
    }
});
test('updates must preserve creation time and advance revision ordering', async () => {
    await assertSucceeds(setDoc(reference('alice'), application));
    await assertFails(updateDoc(reference('alice'), { updatedAt:99 }));
    await assertFails(updateDoc(reference('alice'), { createdAt:101, updatedAt:102 }));
    await assertFails(updateDoc(reference('alice'), { revision:'00000000-0000-0000-0000-000000000000' }));
    await assertSucceeds(updateDoc(reference('alice'), { revision:'00000000-0000-0000-0000-000000000002' }));
    await assertSucceeds(updateDoc(reference('alice'), { updatedAt:101, revision:'00000000-0000-0000-0000-000000000002' }));
});
test('tombstones propagate; hard deletes are forbidden', async () => {
    await assertSucceeds(setDoc(reference('alice'), application));
    await assertSucceeds(updateDoc(reference('alice'), { deleted:true, updatedAt:101 }));
    await assertFails(deleteDoc(reference('alice')));
});
