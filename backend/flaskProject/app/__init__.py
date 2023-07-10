from datetime import timedelta

import boto3
from flask import Flask
from flask_jwt_extended import JWTManager
from flask_sqlalchemy import SQLAlchemy

AWS_ACCESS_KEY="AKIAZ2OAFKUCSFASLA6J"
AWS_SECRET_KEY="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"
S3_BUCKET_NAME="photobombimages"

s3_client = boto3.client('s3',
                         aws_access_key_id=AWS_ACCESS_KEY,
                         aws_secret_access_key=AWS_SECRET_KEY)
bucket_name = S3_BUCKET_NAME

app = Flask(__name__)

app.config['JWT_SECRET_KEY'] = 'secret-key-goes-here'
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = True
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///db.sqlite'
app.config['JWT_ACCESS_TOKEN_EXPIRES'] = timedelta(minutes=15)
app.config['JWT_REFRESH_TOKEN_EXPIRES'] = timedelta(days=30)
db = SQLAlchemy(app)
jwt = JWTManager(app)

db.create_all()

from app.routes.login import login, signup, logout
from app.routes.profile import get_user, delete_user, update_profile_picture
from app.routes.gallery import get_user_collections, add_user_collection, add_image_to_collection, get_collection_images, delete_collection, edit_image_in_collection

