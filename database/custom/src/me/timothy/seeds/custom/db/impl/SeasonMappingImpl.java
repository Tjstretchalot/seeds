package me.timothy.seeds.custom.db.impl;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import me.timothy.seeds.custom.db.SeasonMapping;
import me.timothy.seeds.custom.models.Season;
import me.timothy.seeds.shared.FixedSerializer;
import me.timothy.seeds.shared.db.InlineObjectWithIDMapping;

public class SeasonMappingImpl implements SeasonMapping {
	private InlineObjectWithIDMapping<Season> store;
	
	private static class SeasonSerializer implements FixedSerializer<Season> {
		@Override
		public int write(Season a, ByteBuffer out) {
			ByteBuffer enc = StandardCharsets.ISO_8859_1.encode(a.name);
			out.put((byte)enc.limit());
			out.put(enc);
			return 1 + enc.limit();
		}

		@Override
		public Season read(int id, ByteBuffer in) {
			final int oldLim = in.limit();
			byte len = in.get();
			in.limit(in.position() + len);
			String name = StandardCharsets.ISO_8859_1.decode(in).toString();
			in.limit(oldLim);
			return new Season(id, name);
		}

		@Override
		public int maxSize() {
			return 64;
		}
	}
	
	public SeasonMappingImpl(String filePath) {
		store = new InlineObjectWithIDMapping<>(filePath, new SeasonSerializer());
		store.open();
	}
	
	@Override
	public Season fetchByID(int id) {
		return store.get(id);
	}

	@Override
	public Iterator<Season> fetchIter() {
		return store.iterAll();
	}

	@Override
	public void open() {
		store.open();
	}

	@Override
	public void flush() {
		store.flush();
	}

	@Override
	public void close() {
		store.close();
	}
}
