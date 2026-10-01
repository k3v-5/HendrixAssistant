import json
import base64
import pytest
from unittest.mock import patch, MagicMock
from automation.windows_credential_vault import WindowsCredentialVault

def test_extract_jwt_email():
    payload = {"email": "user@domain.com", "name": "Test User"}
    payload_b64 = base64.urlsafe_b64encode(json.dumps(payload).encode("utf-8")).decode("utf-8").rstrip("=")
    fake_jwt = f"header.{payload_b64}.signature"

    email = WindowsCredentialVault.extract_jwt_email(fake_jwt)
    assert email == "user@domain.com"

def test_extract_jwt_email_invalid():
    assert WindowsCredentialVault.extract_jwt_email("") is None
    assert WindowsCredentialVault.extract_jwt_email("not_a_jwt") is None
    assert WindowsCredentialVault.extract_jwt_email("a.b") is None

def test_credential_real_lifecycle_on_windows():
    vault = WindowsCredentialVault()
    if not vault._advapi32 or not vault.CREDENTIAL:
        pytest.skip("Not running on Windows or advapi32 not available")

    target = "test:temp_pytest_target"
    payload = {"token": {"access_token": "pytest_test_123"}, "id_token": ""}
    blob_str = json.dumps(payload)

    # 1. Write
    write_ok = vault.write_credential(target, blob_str, user_name="pytest_user")
    assert write_ok is True

    # 2. Read
    cred = vault.read_credential(target)
    assert cred is not None
    assert cred["target"] == target
    assert cred["userName"] == "pytest_user"
    assert cred["jsonData"] == payload

    # 3. Delete
    del_ok = vault.delete_credential(target)
    assert del_ok is True

    # 4. Verify deleted
    cred_after = vault.read_credential(target)
    assert cred_after is None
