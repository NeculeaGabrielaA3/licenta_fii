import urllib.parse
from datetime import datetime
from app.utils import create_presigned_url
from flask_login import UserMixin

from app import bucket_name
from . import db

# This is an association table to handle the many-to-many relationship between images and collections
images_collections = db.Table('images_collections',
    db.Column('image_id', db.Integer, db.ForeignKey('image.id'), primary_key=True),
    db.Column('collection_id', db.Integer, db.ForeignKey('collection.id'), primary_key=True)
)

class User(UserMixin, db.Model):
    id = db.Column(db.Integer, primary_key=True)
    email = db.Column(db.String(100), unique=True)
    password = db.Column(db.String(1000))
    first_name = db.Column(db.String(100))
    last_name = db.Column(db.String(100))
    gender = db.Column(db.String(30))
    username = db.Column(db.String(50))
    profile_picture = db.Column(db.String())
    bio = db.Column(db.String(1000))
    images = db.relationship('Image', backref='user', lazy=True)
    collections = db.relationship('Collection', backref='user', lazy=True)

    def update_profile_picture(self, profile_picture):
        self.profile_picture = profile_picture
        db.session.commit()

class Image(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    s3_url = db.Column(db.String(), nullable=False)
    filename = db.Column(db.String(), nullable=False)
    filesize = db.Column(db.Integer, nullable=False)
    uploaded_at = db.Column(db.DateTime, default=db.func.current_timestamp())
    collections = db.relationship('Collection', secondary=images_collections, lazy='subquery',
        backref=db.backref('images', lazy=True))

    def serialize(self):
        return {
            'id': self.id,
            'user_id': self.user_id,
            's3_url': create_presigned_url(bucket_name, urllib.parse.urlparse(self.s3_url).path[1:]),
            'filename': self.filename,
            'filesize': self.filesize,
            'uploaded_at': self.uploaded_at.isoformat() if isinstance(self.uploaded_at, datetime) else None,
        }

class Collection(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(64))
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'))

# db.create_all()