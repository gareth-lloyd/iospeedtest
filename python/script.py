import argparse, json, eventlet
from eventlet.green import urllib2
from urllib import urlencode

from pymongo import Connection

MONGO_HOST, MONGO_PORT, MONGO_DBNAME = 'localhost', 27017, 'iotest'
INSERT_SAFELY = False

MONGO_MAX_CONNECTIONS = 1
conn = Connection(MONGO_HOST, MONGO_PORT,
            max_pool_size=MONGO_MAX_CONNECTIONS)

DB = getattr(conn, MONGO_DBNAME)
POSTS, COMMENTS = DB.posts, DB.comments


def init_db():
    POSTS.ensure_index('uid', unique=True)
    POSTS.ensure_index('source_id')
    COMMENTS.ensure_index('uid', unique=True)
    COMMENTS.ensure_index('post_id')


class Model(object):
    def __init__(self, **kwargs):
        for attr_name in self.ATTRS:
            setattr(self, attr_name, kwargs.get(attr_name))

    def save(self):
        doc = {k: getattr(self, k) for k in self.ATTRS}
        self.COLLECTION.insert(doc, safe=INSERT_SAFELY)


class Post(Model):
    ATTRS = ['uid', 'source_id', 'text']
    COLLECTION = POSTS


class Comment(Model):
    ATTRS = ['uid', 'post_id', 'text']
    COLLECTION = COMMENTS


class GraphClient(object):
    HOST = "https://graph.facebook.com"

    def __init__(self, token):
        self.token = token

    def _graph_url(self, *path_bits, **query_params):
        path_bits = list(path_bits)
        path_bits.insert(0, self.HOST)
        query_params.update(access_token=self.token)
        return "/".join(path_bits) + "?" + urlencode(query_params.items())

    def graph_call(self, *path_bits, **kwargs):
        url = self._graph_url(*path_bits, **kwargs)
        resp = urllib2.urlopen(url)
        return json.loads(resp.read())['data']


class PollingTask(object):
    def __init__(self, pool, source_id, client):
        self.pool, self.source_id, self.client = pool, source_id, client

    def go(self):
        for post in self.get_posts():
            self.pool.spawn(self.get_comments, post)

    def get_posts(self):
        post_dicts = self.client.graph_call(self.source_id, 'posts')
        posts = []
        for p in post_dicts:
            text = p['message'] if 'message' in p else p['story'] if 'story' in p else p['caption']
            post = Post(uid=p['id'], text=text, source_id=self.source_id)
            post.save()
            posts.append(post)

        return posts

    def get_comments(self, post):
        comment_dicts = self.client.graph_call(post.uid, 'comments')

        for c in comment_dicts:
            comment = Comment(uid=c['id'], text=c['message'], post_id=post.uid)
            comment.save()


def poll(pool, source_id, client):
    PollingTask(pool, source_id, client).go()


def main():
    parser = argparse.ArgumentParser(description='Test polling performance')

    parser.add_argument('-d', '--dropdb', action='store_true',
            help='drop the mongo database before starting')
    parser.add_argument('token', type=str, nargs='?', help='a facebook token')
    args = parser.parse_args()

    token = args.token
    if args.dropdb:
        print 'dropping the database'
        conn.drop_database(MONGO_DBNAME)
    init_db()

    with open('../files/source_ids.txt', 'r') as f:
        source_ids = [line.strip() for line in f.readlines()]

    client = GraphClient(token)

    pool = eventlet.GreenPool(100)
    for source_id in source_ids:
        pool.spawn(poll, pool, source_id, client)

    pool.waitall()


if __name__ == "__main__":
    main()

