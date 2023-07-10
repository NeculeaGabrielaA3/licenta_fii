import re
import boto3
from botocore.exceptions import NoCredentialsError
from werkzeug.utils import secure_filename
from app import s3_client, bucket_name
from app import AWS_ACCESS_KEY, AWS_SECRET_KEY

ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'tmp'}
def validate_password(password):
    if len(password) < 8:
        return False
    if not re.search("[a-z]", password):
        return False
    if not re.search("[A-Z]", password):
        return False
    if not re.search("[0-9]", password):
        return False
    if not re.search("[!@#$%^&*.]", password):
        return False
    return True

def valid_image_extension(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def validate_email(email):
    email_regex = r'^[a-z0-9]+[\._]?[a-z0-9]+[@]\w+[.]\w{2,3}$'
    if re.search(email_regex, email):
        return True
    else:
        return False

def create_presigned_url(bucket_name, object_name, expiration=3600):
    s3_client2 = boto3.client('s3', region_name='eu-central-1',
                             aws_access_key_id=AWS_ACCESS_KEY,
                             aws_secret_access_key=AWS_SECRET_KEY,
                             config=boto3.session.Config(signature_version='s3v4'))

    try:
        response = s3_client2.generate_presigned_url('get_object',
                                                    Params={'Bucket': bucket_name,
                                                            'Key': object_name},
                                                    ExpiresIn=expiration)
    except NoCredentialsError:
        return None

    return response

def upload_image_to_s3(image, user_id):
    s3_key = f'user_{user_id}/{secure_filename(image.filename)}'
    s3_client.upload_fileobj(image, bucket_name, s3_key, ExtraArgs={'ContentType': image.content_type})
    s3_url = f'https://{bucket_name}.s3.amazonaws.com/{s3_key}'
    return s3_url